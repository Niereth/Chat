package model;

public interface Observed {

    void addObserver(Observer observer);

    void removeObserver(Observer observer);
}
