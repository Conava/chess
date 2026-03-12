package io.github.conava.chess.application.i18n;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {

    public enum Language {EN, DE}

    private final ObjectProperty<Language> currentLanguage = new SimpleObjectProperty<>(Language.EN);
    private ResourceBundle bundle;

    public I18n(Language language) {
        setLanguage(language);
    }

    public void setLanguage(Language language) {
        currentLanguage.set(language);
        Locale locale = language == Language.EN ? Locale.ENGLISH : Locale.GERMAN;
        bundle = ResourceBundle.getBundle("i18n/messages", locale);
    }

    /**
     * Returns the localised string for the given key.
     *
     * @param key the resource bundle key.
     * @return the localised string.
     */
    public String get(String key) {
        return bundle.getString(key);
    }

    /**
     * Returns the localised string for the given key, formatted with the supplied arguments.
     *
     * <p>The pattern string may contain {@link MessageFormat} placeholders such as {@code {0}}
     * and {@code {1}}.</p>
     *
     * @param key  the resource bundle key whose value contains a {@link MessageFormat} pattern.
     * @param args the arguments to substitute into the pattern.
     * @return the formatted localised string.
     */
    public String get(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public Language getLanguage() {
        return currentLanguage.get();
    }

    public ObjectProperty<Language> currentLanguageProperty() {
        return currentLanguage;
    }
}
