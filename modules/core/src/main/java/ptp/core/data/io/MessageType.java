package ptp.core.data.io;

public enum MessageType {
    CREATE_GAME,
    JOIN_GAME,
    JOIN_CODE,
    SUBMIT_MOVE,
    MOVE_FEEDBACK,
    MOVE_FROM_REMOTE,
    GAME_STATUS,
    GAME_END,
    SUCCESS,
    ERROR,
    FAILURE
}