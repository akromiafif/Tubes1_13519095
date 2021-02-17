package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;


import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private final Random random;
    private final GameState gameState;
    private final Opponent opponent;
    private final MyWorm currentWorm;
    private final MyPlayer myPlayer;
    private final Cell[][] myMap;
    private final List<Position> powerUpPosition;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
        this.myMap = gameState.map;
        this.powerUpPosition = getPowerUpPosition();
        this.myPlayer = gameState.myPlayer;
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == myPlayer.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {

        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }

        int powerUpIdx = 0;
        Position healthPack = new Position();
        if (powerUpPosition.size() > 0) {
            powerUpIdx = random.nextInt(powerUpPosition.size());
            healthPack = powerUpPosition.get(powerUpIdx);
        }

        Position center = new Position();
        center.x = 17;
        center.y = 17;
        if (powerUpPosition.size() > 0){
            return DigAndMove(healthPack);
        }
        else {
            return DigAndMove(center);
        }
    }


    private Command DigAndMove(Position destination) {
        Position Mywormpost = currentWorm.position;
        Position NextCell = FindNextCellinPath(Mywormpost,destination);

        Cell block = gameState.map[NextCell.y][NextCell.x];
        if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }
        else {
            return  new DoNothingCommand();
        }
    }

    public Position FindNextCellinPath (Position origin, Position destination){
        Position NextPos = new Position();

        if (origin.x < destination.x){
            NextPos.x = origin.x + 1;
        } else if (origin.x > destination.x){
            NextPos.x = origin.x - 1;
        } else {
            NextPos.x = origin.x;
        }

        if (origin.y < destination.y){
            NextPos.y = origin.y + 1;
        } else if (origin.y > destination.y){
            NextPos.y = origin.y - 1;
        } else {
            NextPos.y = origin.y;
        }

        return NextPos;
    }


    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            if (enemyWorm.health > 0){
                String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
                if (cells.contains(enemyPosition)) {
                    return enemyWorm;
                }
            }
        }

        return null;
    }

    private List<Position> getPowerUpPosition() {
        List<Position> positions = new ArrayList<>();
        for (int i=0; i<gameState.mapSize; i++) {
            for (int j=0; j<gameState.mapSize; j++) {
                PowerUp pwUp = myMap[i][j].powerUp;
                if (pwUp != null) {
                    Position position = new Position();
                    position.x = myMap[i][j].x;
                    position.y = myMap[i][j].y;
                    positions.add(position);
                }
            }
        }

        return positions;
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }
}
