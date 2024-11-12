package by.bsu.dependency.context;

import by.bsu.dependency.annotation.Bean;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.Set;

public class AutoScanApplicationContext extends AbstractApplicationContext {

    /**
     * Создает контекст, содержащий классы из пакета {@code packageName}, помеченные аннотацией {@code @Bean}.
     * <br/>
     * Если имя бина в анноации не указано ({@code name} пустой), оно берется из названия класса.
     * <br/>
     * Подразумевается, что у всех классов, переданных в списке, есть конструктор без аргументов.
     *
     * @param packageName имя сканируемого пакета
     */
    public AutoScanApplicationContext(String packageName) {
        Configuration configuration = new ConfigurationBuilder()
                .forPackage(packageName);
        Set<Class<?>> set = new Reflections(configuration).get(Scanners.SubTypes.of(Scanners.TypesAnnotated.with(Bean.class)).asClass());
        initializeBeanConfigurations(new ArrayList<>(set));
    }

}
