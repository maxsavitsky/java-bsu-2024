package by.bsu.dependency.example;

import by.bsu.dependency.context.ApplicationContext;
import by.bsu.dependency.context.AutoScanApplicationContext;
import by.bsu.dependency.context.HardCodedSingletonApplicationContext;
import by.bsu.dependency.context.SimpleApplicationContext;

public class Main {

    public static void main(String[] args) {
        /*ApplicationContext applicationContext = new SimpleApplicationContext(
                FirstBean.class, OtherBean.class
        );*/
        ApplicationContext applicationContext = new AutoScanApplicationContext(
                "by.bsu.dependency.example"
        );
        applicationContext.start();

        FirstBean firstBean = (FirstBean) applicationContext.getBean("firstBean");
        OtherBean otherBean = (OtherBean) applicationContext.getBean("otherBean");

        firstBean.doSomething();
        otherBean.doSomething();

        // Метод падает, так как в классе HardCodedSingletonApplicationContext не реализовано внедрение зависимостей
        otherBean.doSomethingWithFirst();
    }
}
