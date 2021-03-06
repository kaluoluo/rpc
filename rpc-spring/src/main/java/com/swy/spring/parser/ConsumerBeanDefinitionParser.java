package com.swy.spring.parser;


import com.swy.framework.ServiceConsumer;
import com.swy.register.ZookeeperRegisterCenter;
import com.swy.spring.ProxyFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;



/**
 * 1.将消费者注册到注册中心
 * 2.构建bean
 */
public class ConsumerBeanDefinitionParser implements BeanDefinitionParser {

    private final Class<?> beanClass;

    public ConsumerBeanDefinitionParser(Class<?> beanClass) {
        this.beanClass = beanClass;
    }




    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {

        String interfaces = element.getAttribute("interfaces");
        String ref = element.getAttribute("ref");
        Class clazz = null;
        try {
            clazz = Class.forName(interfaces);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();

        definition.getConstructorArgumentValues().addGenericArgumentValue(clazz);

        definition.setBeanClass(ProxyFactoryBean.class);
        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);

        BeanDefinitionRegistry beanDefinitionRegistry = parserContext.getRegistry();
        beanDefinitionRegistry.registerBeanDefinition(ref,definition);

        //获取服务注册中心
        ZookeeperRegisterCenter registerCenter4Consumer = ZookeeperRegisterCenter.getInstance();

        //将消费者信息注册到注册中心
        ServiceConsumer invoker = new ServiceConsumer();

        invoker.setConsumer(clazz);
        invoker.setServiceObject(interfaces);
        invoker.setGroupName("");
        registerCenter4Consumer.registerConsumer(invoker);

        return definition;
    }
}
