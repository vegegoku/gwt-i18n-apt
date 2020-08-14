/*
 * Copyright © 2018 The GWT Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gwtproject.i18n.processor;

import java.util.Collection;

/** Definition of a single entry for a resource. */
public interface ResourceEntry {

  /**
   * Retrieve a particular form for this entry.
   *
   * @param form form to retrieve (null for the default)
   * @return null if the requested form is not present
   */
  String getForm(String form);

  /**
   * Returns a list of forms associated with this entry.
   *
   * <p>The default form (also the only form for anything other than messages with plural support)
   * is always available and not present in this list.
   */
  Collection<String> getForms();

  /** Returns key for this entry (must not be null). */
  String getKey();
}
