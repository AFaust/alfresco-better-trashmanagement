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
package de.axelfaust.alfresco.trash.management.repo.rendition;

import org.alfresco.repo.rendition.RenditionServiceImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class RenditionServiceBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor
{

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException
    {
        final BeanDefinition renditionServiceDefinition = registry.getBeanDefinition("renditionService");
        if (renditionServiceDefinition != null
                && renditionServiceDefinition.getBeanClassName().equals(RenditionServiceImpl.class.getName()))
        {
            renditionServiceDefinition.setBeanClassName(ArchiveSupportingRenditionServiceImpl.class.getName());
            renditionServiceDefinition.getPropertyValues().add("nodeArchiveService", new RuntimeBeanReference("nodeArchiveService"));
        }
    }

}
