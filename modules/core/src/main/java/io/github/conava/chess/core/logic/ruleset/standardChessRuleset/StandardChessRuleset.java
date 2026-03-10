package io.github.conava.chess.core.logic.ruleset.standardChessRuleset;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.logic.ruleset.AbstractChessRuleset;
import io.github.conava.chess.core.logic.ruleset.possibleStartPositions.PossibleStandardPosition;

/**
 * Standard chess ruleset.
 *
 * <p>All shared chess logic lives in {@link AbstractChessRuleset}. This class only
 * overrides {@link #getStartBoard} to return the standard starting position.
 */
public class StandardChessRuleset extends AbstractChessRuleset {

    /**
     * Returns the standard chess starting position.
     *
     * @param player1 Player with the white pieces
     * @param player2 Player with the black pieces
     * @return Double array of the board. Starting with y as the first position.
     */
    @Override
    public Square[][] getStartBoard(Player player1, Player player2) {
        PossibleStandardPosition position = new PossibleStandardPosition(player1, player2);
        return position.getStartBoard();
    }
}
