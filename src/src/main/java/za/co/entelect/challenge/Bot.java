package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;
import za.co.entelect.challenge.enums.Profession;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private final Random random;
    private final GameState gameState;
    private final Opponent opponent;
    private final MyWorm currentWorm;
    private final Cell[][] myMap;
    private final List<Position> powerUpPosition;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
        this.myMap = gameState.map;
        this.powerUpPosition = getPowerUpPosition();
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {
        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);

        }

        return Strategy(currentWorm);

    }
    public Command Strategy (MyWorm myWorm){
        Worm Target = NearestEnemy(currentWorm);
        if (myWorm.profession != Profession.COMMANDO) {
            if (CanSnowBall(currentWorm, Target)) {
                return new SnowBallCommand(Target.position.x, Target.position.y);
            } else if (CanBananaBomb(currentWorm, Target)) {
                return new BananaCommand(Target.position.x, Target.position.y);
            } else {

                Position healthPack = new Position();
                if (powerUpPosition.size() > 0 && myWorm.health < 50) {
                    healthPack = NearestPowerUp();
                    return DigAndMove(healthPack);
                } else {
                    if (myWorm.health > 80){
                        if(currentWorm.position.x < 13 && currentWorm.position.y < 13){
                            return DigAndMove(healthPack);
                        }
                        else {
                            return Kill_Agent();
                        }
                    }
                    else {
                        return Hunt();
                    }
                }
            }
        }
        else {
            Position healthPack = new Position();
            if (powerUpPosition.size() > 0 ) {
                healthPack = NearestPowerUp();
                return DigAndMove(healthPack);
            } else {
                return Hunt();
            }
        }
    }

    public Command DigAndMove(Position destination) {
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

    public boolean CanSnowBall(MyWorm myWorm, Worm enemyWorm) {
        return  (myWorm.profession == Profession.TECHNOLOGIST)
                && (myWorm.snowballs.count > 0)
                && (enemyWorm.roundsUntilUnfrozen == 0)
                && (Distance(myWorm.position, enemyWorm.position) < myWorm.snowballs.range)
                && (Distance(myWorm.position, enemyWorm.position) > myWorm.snowballs.freezeRadius);
    }

    public boolean CanBananaBomb (MyWorm myWorm, Worm enemyWorm){
        return (myWorm.profession == Profession.AGENT)
                && (myWorm.bananaBombs.count > 0)
                && (Distance(myWorm.position, enemyWorm.position) <= myWorm.bananaBombs.range)
                && (Distance(myWorm.position, enemyWorm.position) > myWorm.bananaBombs.damageRadius);
    }

    public Position NearestPowerUp (){
        int min = 1000;
        Position NearestPowerUpPosition = new Position();
        for (Position Nearest : powerUpPosition){
            if (Distance(currentWorm.position,Nearest) < min){
                min = Distance(currentWorm.position,Nearest);
                NearestPowerUpPosition = Nearest;
            }
        }
        return NearestPowerUpPosition;
    }

    public Position FindNextCellinPath (Position origin, Position destination){
        Position NextPos = new Position();
        if (origin.x < destination.x){
            NextPos.x = origin.x + 1;
        }
        else if (origin.x > destination.x){
            NextPos.x = origin.x - 1;
        }
        else {
            NextPos.x = origin.x;
        }

        if (origin.y < destination.y){
            NextPos.y = origin.y + 1;
        }
        else if (origin.y > destination.y){
            NextPos.y = origin.y - 1;
        }
        else {
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
        Worm[] myWorms = gameState.myPlayer.worms;
        Position pos1 = myWorms[0].position;


        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }
                if (coordinateX == pos1.x && coordinateY == pos1.y){
                    break;
                }
                if (myWorms.length > 1){
                    Position pos2 = myWorms[1].position;
                    if (coordinateX == pos2.x && coordinateY == pos2.y) {
                        break;
                    }
                }

                if (myWorms.length>2){
                    Position pos3 = myWorms[2].position;
                    if (coordinateX == pos3.x && coordinateY == pos3.y){
                    break;
                }
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

    private int Distance (Position a, Position b){
        double dx = Math.pow(a.x - b.x,2);
        double dy = Math.pow(a.y - b.y,2);
        return (int) (Math.sqrt((dx + dy)));
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

    public Command Hunt(){
        Position TargetPosition = NearestEnemy(currentWorm).position;
        return DigAndMove(TargetPosition);
    }

    public Worm NearestEnemy (MyWorm myWorm) {
        Position MyWormPosition = myWorm.position;
        Worm Target = new Worm();
        int min = 1000;
        for (Worm EnemyWorm : opponent.worms){
            if (EnemyWorm.health > 0){
                int temp = Distance(MyWormPosition, EnemyWorm.position);
                if (temp < min){
                    min = temp;
                    Target = EnemyWorm;
                }
            }
        }
        return Target;
    }

    public boolean CheckDirection(MyWorm myWorm, int vertical, int horizontal){
        boolean flag = true;
        Cell cell;
        Worm[] TeamWorm = gameState.myPlayer.worms;
        int i = currentWorm.position.x;
        int k = currentWorm.position.y;

        while(i <= currentWorm.position.x + 4 && k <= currentWorm.position.y + 4) {
                for(int j = 0;j < TeamWorm.length;j++) {
                    cell = gameState.map[k][i];
                    if ((TeamWorm[i].position.x == cell.x && TeamWorm[i].position.y == cell.y) || (cell.type == CellType.DIRT)) {
                        flag = false;
                    }
                }

                i += horizontal;
                k += vertical;
        }

        return flag;
    }


    public boolean CheckClearDirection(Direction dir){
        boolean flag = true;
        int horizontal = dir.x;
        int vertical = dir.y;
        Cell cell;
        Worm[] TeamWorm = gameState.myPlayer.worms;
        int i = currentWorm.position.x;
        int k = currentWorm.position.y;
        boolean clear = false;
        while(i <= currentWorm.position.x + currentWorm.weapon.range && k <= currentWorm.position.y + currentWorm.weapon.range && (!clear) ) {
            for (int j = 0;j < TeamWorm.length;j++){
                if (isValidCoordinate(i,k)) {
                    cell = gameState.map[k][i];
                    if ((TeamWorm[j].position.x == cell.x && TeamWorm[j].position.y == cell.y) || (cell.type == CellType.DIRT)) {

                        flag = false;
                    }
                    Worm enemyWorm = NearestEnemy(currentWorm);
                    if (enemyWorm.position.x == cell.x && enemyWorm.position.y == cell.y) {
                        clear = true;
                    }
                    i += horizontal;
                    k += vertical;
                }
            }
        }

        return flag;
    }
    public Command Kill_Agent (){
        Worm Target = new Worm();
        for (Worm EnemyWorm : opponent.worms){
            if (EnemyWorm.health > 0 && EnemyWorm.profession == Profession.COMMANDO){
                Target = EnemyWorm;
                }
            }
        if (Target.profession != Profession.COMMANDO){
            return Hunt();
        }
        else {
            return DigAndMove(Target.position);
        }
        }

    }



