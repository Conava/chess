package ptp.project.logic.game;

public abstract class Observable {
    private GameObserver observer;


    public void addObserver(GameObserver observer) {
    }

    public void removeObserver(GameObserver observer) {
    }

    public void notifyObservers() {
        observer.update();
    }
}
