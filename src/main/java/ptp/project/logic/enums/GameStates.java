package ptp.project.logic.enums;

public enum GameStates {

    NONE,

    RUNNING,

    WHITE_WIN_SURRENDER,

    WHITE_WIN_CHECKMATE,

    BLACK_WIN_SURRENDER,

    BLACK_WIN_CHECKMATE,

    DRAW_AGREEMENT,

    //insufficient material
    DRAW_MATERIAL,

    DRAW_STALEMATE,

    //50-move-rule
    DRAW_MOVE_RULE,

    //3 times same position
    DRAW_REPETITION
}