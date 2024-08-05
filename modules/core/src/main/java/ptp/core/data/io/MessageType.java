package ptp.core.data.io;

public enum MessageType {
    CREATE_GAME,
    JOIN_GAME,
    SUBMIT_MOVE,
    MOVE_FEEDBACK,
    MOVE_FROM_REMOTE,
    GAME_STATUS,
    GAME_END,
    SUCCESS,
    FAILURE
}