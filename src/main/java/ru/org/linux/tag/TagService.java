/*
 * Copyright 1998-2013 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.tag;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import ru.org.linux.topic.TopicTagService;
import ru.org.linux.user.UserErrorException;

import java.util.*;

@Service
public class TagService {
  private static final Logger logger = LoggerFactory.getLogger(TagService.class);

  @Autowired
  private TagDao tagDao;

  private final List<ITagActionHandler> actionHandlers = new ArrayList<>();

  public List<ITagActionHandler> getActionHandlers() {
    return actionHandlers;
  }

  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tag название тега
   * @return идентификационный номер
   * @throws TagNotFoundException
   */
  public int getTagId(String tag) throws TagNotFoundException {
    Optional<Integer> tagId = tagDao.getTagId(tag);

    if (tagId.isPresent()) {
      return tagId.get();
    } else {
      throw new TagNotFoundException();
    }
  }

  /**
   * Получить список наиболее популярных тегов.
   *
   * @return список наиболее популярных тегов
   */
  public SortedSet<String> getTopTags() {
    return tagDao.getTopTags();
  }

  /**
   * Получить уникальный список первых букв тегов.
   *
   * @return список первых букв тегов
   */
  public SortedSet<String> getFirstLetters() {
    return tagDao.getFirstLetters();
  }

  /**
   * Получить список тегов по префиксу.
   *
   * @param prefix     префикс
   * @return список тегов по первому символу
   */
  public Map<String, Integer> getTagsByPrefix(String prefix, int threshold) {
    ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();

    for (TagInfo info : tagDao.getTagsByPrefix(prefix, threshold)) {
      builder.put(info.name(), info.topicCount());
    }

    return builder.build();
  }

  /**
   * Получить список популярных тегов по префиксу.
   *
   * @param prefix     префикс
   * @param count      количество тегов
   * @return список тегов по первому символу
   */
  public SortedSet<String> suggestTagsByPrefix(String prefix, int count) {
    return tagDao.getTopTagsByPrefix(prefix, 2, count);
  }

  /**
   * Изменить название существующего тега.
   *
   * @param oldTagName старое название тега
   * @param tagName    новое название тега
   * @param errors     обработчик ошибок ввода для формы
   */
  public void change(String oldTagName, String tagName, Errors errors) {
    try {
      TagName.checkTag(tagName);
      int oldTagId = getTagId(oldTagName);

      if (tagDao.getTagId(tagName).isPresent()) {
        errors.rejectValue("tagName", "", "Тег с таким именем уже существует!");
      } else {
        tagDao.changeTag(oldTagId, tagName);
        logger.info(
                "Изменено название тега. Старое значение: '{}'; новое значение: '{}'",
                oldTagName,
                tagName
        );
      }
    } catch (UserErrorException e) {
      errors.rejectValue("tagName", "", e.getMessage());
    } catch (TagNotFoundException e) {
      errors.rejectValue("tagName", "", "Тега с таким именем не существует!");
    }
  }

  /**
   * Удалить тег по названию. Заменить все использования удаляемого тега
   * новым тегом (если имя нового тега не null).
   *
   * @param tagName    название тега
   * @param newTagName новое название тега
   * @param errors     обработчик ошибок ввода для формы
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void delete(String tagName, String newTagName, Errors errors) {
    try {
      int oldTagId = getTagId(tagName);
      if (!Strings.isNullOrEmpty(newTagName)) {
        if (newTagName.equals(tagName)) {
          errors.rejectValue("tagName", "", "Заменяемый тег не должен быть равен удаляемому!");
          return;
        }
        TagName.checkTag(newTagName);
        int newTagId = getOrCreateTag(newTagName);

        for (ITagActionHandler actionHandler : actionHandlers) {
          actionHandler.replaceTag(oldTagId, tagName, newTagId, newTagName);
        }
        logger.debug("Удаляемый тег '{}' заменён тегом '{}'", tagName, newTagName);
      }
      for (ITagActionHandler actionHandler : actionHandlers) {
        actionHandler.deleteTag(oldTagId, tagName);
      }
      tagDao.deleteTag(oldTagId);
      logger.info("Удалён тег: " + tagName);
    } catch (UserErrorException e) {
      errors.rejectValue("tagName", "", e.getMessage());
    } catch (TagNotFoundException e) {
      errors.rejectValue("tagName", "", "Тега с таким именем не существует!");
    }
  }

  /**
   * Получение идентификационного номера тега по названию, либо создание нового тега.
   *
   * @param tagName название тега
   * @return идентификационный номер тега
   */
  public int getOrCreateTag(final String tagName) {
    return tagDao.getTagId(tagName).or(new Supplier<Integer>() {
      @Override
      public Integer get() {
        return tagDao.createTag(tagName);
      }
    });
  }

  public static String toString(Collection<String> tags) {
    return Joiner.on(",").join(tags);
  }

  /**
   * пересчёт счётчиков использования.
   */
  public void reCalculateAllCounters() {
    for (ITagActionHandler actionHandler : actionHandlers) {
      actionHandler.reCalculateAllCounters();
    }
  }

  public TagInfo getTagInfo(String tag, boolean skipZero) throws TagNotFoundException {
    Optional<Integer> tagId = tagDao.getTagId(tag, skipZero);

    if (!tagId.isPresent()) {
      throw new TagNotFoundException();
    }

    return tagDao.getTagInfo(tagId.get());
  }

  public List<TagRef> getRelatedTags(int tagId) {
    return Ordering.natural().immutableSortedCopy(TopicTagService.namesToRefs(tagDao.relatedTags(tagId)));
  }
}
