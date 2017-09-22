/*
 * Copyright 2017 Axel Faust
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.axelfaust.alfresco.trash.management.repo;

import org.alfresco.service.namespace.QName;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public interface BetterTrashManagementModel
{

    String NAMESPACE_URK = "http://axelfaust.de/model/beterTrashManagement/1.0";

    String NAMESPACE_PREFIX = "btmm";

    QName ASPECT_USER_READ_ACCESS_GRANTED = QName.createQName(NAMESPACE_URK, "userReadAccessGranted");

    QName PROP_READ_ACCESS_GRANTED_TO = QName.createQName(NAMESPACE_URK, "readAccessGrantedTo");
}
