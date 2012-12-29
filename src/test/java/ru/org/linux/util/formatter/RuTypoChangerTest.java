/*
 * Copyright 1998-2012 Linux.org.ru
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
package ru.org.linux.util.formatter;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * тесты для {@link RuTypoChanger}
 */

@RunWith(value = Parameterized.class)
public class RuTypoChangerTest {
  private final String inputString;
  private final String expectedResult;

  public RuTypoChangerTest(String inputString, String expectedResult) {
    this.inputString = inputString;
    this.expectedResult = expectedResult;
  }


  @Test
  public void checkQuotesDecorator() {
    // given
    RuTypoChanger ruTypoChanger = new RuTypoChanger();

    // when
    String actualResult = ruTypoChanger.format(inputString);

    // then
    Assert.assertEquals(expectedResult, actualResult);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    Object[][] data = new Object[][]{
            {"две кавычки вместе: \"\"", "две кавычки вместе: &laquo;&raquo;"},
            {"пробел между кавычками: \" \"", "пробел между кавычками: &quot; &quot;"},
            {"\"слово\" в кавычках", "&laquo;слово&raquo; в кавычках"},
            {"\"фраза в кавычках\"", "&laquo;фраза в кавычках&raquo;"},
            {"\" слово \" с пробелами", "&quot; слово &quot; с пробелами"},
            {"\" фраза с пробелами \"", "&quot; фраза с пробелами &quot;"},
            {"\" фраза с пробелом  в начале\"", "&quot; фраза с пробелом  в начале&quot;"},
            {"\"фраза с пробелом  в конце \"", "&laquo;фраза с пробелом  в конце &raquo;"},
            {"\"вложенные кавычки \"в конце\"\"", "&laquo;вложенные кавычки &bdquo;в конце&ldquo;&raquo;"},
            {"\"\"вложенные кавычки\" в начале\"", "&laquo;&quot;вложенные кавычки&raquo; в начале&quot;"},
            {"\"\"\"\"\"\"\"\"много непарных кавычек в начале\"", "&laquo;&raquo;&quot;&quot;&quot;&quot;&quot;&quot;много непарных кавычек в начале&quot;"},
            {"\"много непарных кавычек в конце\"\"\"\"\"\"\"\"", "&laquo;много непарных кавычек в конце&raquo;&quot;&quot;&quot;&quot;&quot;&quot;&quot;"},
    };
    return Arrays.asList(data);
  }

}
