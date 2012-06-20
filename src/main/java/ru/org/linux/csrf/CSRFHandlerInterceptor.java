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
package ru.org.linux.csrf;

import com.google.common.base.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CSRFHandlerInterceptor extends HandlerInterceptorAdapter {
  private final static Log logger = LogFactory.getLog(CSRFHandlerInterceptor.class);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    if (!request.getMethod().equalsIgnoreCase("POST")) {
      // Not a POST - allow the request
      return true;
    } else {
      // This is a POST request - need to check the CSRF token
      //CSRFProtectionService.checkCSRF(request);

      String csrfInput = request.getParameter(CSRFProtectionService.CSRF_INPUT_NAME);

      if (Strings.isNullOrEmpty(csrfInput)) {
        logger.debug("Missing CSRF field for " + request.getRequestURI());
      }

      return true;
    }
  }
}
