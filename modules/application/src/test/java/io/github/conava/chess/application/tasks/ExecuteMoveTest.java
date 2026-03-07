package io.github.conava.chess.application.tasks;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.Pieces;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ExecuteMoveTest {

    @Test
    void callsMoveOnRegularMove() throws Exception {
        Chess chess = mock(Chess.class);
        Square from = new Square(6, 4);
        Square to   = new Square(4, 4);

        new ExecuteMove(chess, from, to, null).call();

        verify(chess).movePiece(from, to);
        verify(chess, never()).promoteMove(any(), any(), any());
    }

    @Test
    void callsPromoteMoveOnPromotion() throws Exception {
        Chess chess = mock(Chess.class);
        Square from = new Square(1, 4);
        Square to   = new Square(0, 4);

        new ExecuteMove(chess, from, to, Pieces.QUEEN).call();

        verify(chess).promoteMove(from, to, Pieces.QUEEN);
        verify(chess, never()).movePiece(any(), any());
    }
}
