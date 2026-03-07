package io.github.conava.chess.application.i18n;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {

    public enum Language { EN, DE }

    private final ObjectProperty<Language> currentLanguage =
            new SimpleObjectProperty<>(Language.EN);
    private ResourceBundle bundle;

    public I18n(Language language) {
        setLanguage(language);
    }

    public void setLanguage(Language language) {
        currentLanguage.set(language);
        Locale locale = language == Language.EN ? Locale.ENGLISH : Locale.GERMAN;
        bundle = ResourceBundle.getBundle("i18n/messages", locale);
    }

    public String get(String key) {
        return bundle.getString(key);
    }

    public ResourceBundle getBundle()                          { return bundle; }
    public Language getLanguage()                              { return currentLanguage.get(); }
    public ObjectProperty<Language> currentLanguageProperty() { return currentLanguage; }
}
