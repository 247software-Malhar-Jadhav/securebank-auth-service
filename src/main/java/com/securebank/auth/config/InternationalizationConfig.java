package com.securebank.auth.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Internationalisation wiring for en / hi / mr.
 *
 * <p>{@link MessageSource} loads the {@code i18n/messages*.properties} bundles; the
 * exception handler and validation messages resolve keys through it. The
 * {@link LocaleResolver} picks the request locale from the {@code Accept-Language}
 * header (the gateway forwards the user's preferred_locale there), defaulting to en and
 * restricting to the three supported languages.
 */
@Configuration
public class InternationalizationConfig {

    private static final Locale EN = Locale.forLanguageTag("en");
    private static final Locale HI = Locale.forLanguageTag("hi");
    private static final Locale MR = Locale.forLanguageTag("mr");

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:i18n/messages");
        ms.setDefaultEncoding("UTF-8");          // crucial for Devanagari
        ms.setFallbackToSystemLocale(false);     // unknown locale -> our 'en' default
        ms.setUseCodeAsDefaultMessage(true);     // never NPE on a missing key
        return ms;
    }

    /**
     * Make Bean Validation resolve message keys (e.g. {validation.locale.invalid})
     * through our localised {@link MessageSource}, so constraint violations are also
     * translated to en/hi/mr instead of using the static default text.
     */
    @Bean
    public LocalValidatorFactoryBean getValidator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(EN);
        resolver.setSupportedLocales(List.of(EN, HI, MR));
        return resolver;
    }
}
