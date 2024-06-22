package ptp.project.logic.gameTemp;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.*;
import ptp.project.logic.game.Observable;
import ptp.project.logic.movesTemp.MoveTemp;
import ptp.project.logic.piecesTemp.PieceTemp;
import ptp.project.logic.rulesetTemp.StandardChessRulesetTemp;

import java.util.ArrayList;
import java.util.List;

public class OfflineGameTemp extends Observable implements GameTemp {
    PlayerTemp playerTemp1;
    PlayerTemp playerTemp2;
    RulesetTemp rulesetTemp;
    BoardTemp boardTemp;
    int turnCount; //even turn count means white to move
    List<MoveTemp> moveTemps;

    public OfflineGameTemp() {
        playerTemp1 = new PlayerTemp("Player 1", "white");
        playerTemp2 = new PlayerTemp("Player 2", "black");
        this.rulesetTemp = new StandardChessRulesetTemp(); //current default
        this.boardTemp = new BoardTemp(rulesetTemp.getStartBoard(playerTemp1, playerTemp2));
        this.turnCount = 0;
        this.moveTemps = new ArrayList<>();
    }

    @Override
    public void start() {

    }

    @Override
    public PlayerTemp getCurrentPlayer() {
        return turnCount % 2 == 0 ? playerTemp1 : playerTemp2;
    }

    @Override
    public PlayerTemp getPlayerWhite() {
        return playerTemp1;
    }

    @Override
    public PlayerTemp getPlayerBlack() {
        return playerTemp2;
    }

    @Override
    public RulesetTemp getRuleset() {
        return rulesetTemp;
    }

    @Override
    public PieceTemp getPieceAt(SquareTemp notation) {
        return null;
    }

    @Override
    public List<SquareTemp> getLegalSquares(SquareTemp squareTemp) {

        List<SquareTemp> legalSquareTemps = new ArrayList<>();
        if (squareTemp != null && !squareTemp.isOccupiedBy().equals(getCurrentPlayer())) {
            return legalSquareTemps;
        }
        int moveAmount = rulesetTemp.getLegalMoves(squareTemp, boardTemp, moveTemps, playerTemp1, playerTemp2).size();
        MoveTemp moveTemp;
        for (int i = 0; i < moveAmount; i++) {
            moveTemp = rulesetTemp.getLegalMoves(squareTemp, boardTemp, moveTemps, playerTemp1, playerTemp2).get(i);
            legalSquareTemps.add(moveTemp.getEnd());
        }
        return legalSquareTemps;
    }

    @Override
    public void movePiece(SquareTemp squareTempStart, SquareTemp squareTempEnd) throws IllegalMoveException {
        PlayerTemp playerTemp = this.getCurrentPlayer();
        MoveTemp moveTemp =  new MoveTemp(squareTempStart, squareTempEnd);

        if (rulesetTemp.isValidSquare(squareTempStart)) {
            if (squareTempStart.isOccupiedBy() != null && squareTempStart.isOccupiedBy().equals(playerTemp)) {
                if (this.getLegalSquares(squareTempStart).contains(squareTempEnd)) {
                    boardTemp.executeMove(moveTemp);
                    moveTemps.add(moveTemp);
                    turnCount++;
                    return; //update here
                }
            }
        }
        throw new IllegalMoveException(moveTemp);
    }

    @Override
    public List<MoveTemp> getMoveList() {
        return moveTemps;
    }

    private SquareTemp toBoardSquare(SquareTemp squareTemp) {
        return boardTemp.getSquare(squareTemp.getY(), squareTemp.getX());
    }
}