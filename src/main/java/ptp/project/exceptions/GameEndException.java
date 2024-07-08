package ptp.project.exceptions;

import ptp.project.data.enums.GameState;
import ptp.project.logic.moves.Move;

public class GameEndException extends Exception{
    public GameEndException(GameState gameState) {
        super();
    }
}