import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

class Player {
  static Map map;
  static Base myBase;

  static void debug(Object obj) {
    System.err.println(obj);
  }

  public static void main(String args[]) {
    Scanner in = new Scanner(System.in);
    myBase = new Base(in.nextInt(), in.nextInt());
    in.nextInt(); // nbHeros
    map = new Map();
    map.initSpaces();

    // game loop
    while (true) {
      init(in);

      // Game Logic
      map.computeScores();

      // takeAction for each hero
      for (Hero hero : map.heros) {
        hero.takeAction();
      }

    }
  }

  private static void init(Scanner in) {
    map.reset();
    myBase.health = in.nextInt();
    myBase.mana = in.nextInt();
    int hisBaseHealth = in.nextInt();
    int hisBaseMana = in.nextInt();

    int entityCount = in.nextInt(); // Amount of heros and monsters you can see
    for (int i = 0; i < entityCount; i++) {
      int id = in.nextInt(); // Unique identifier
      int type = in.nextInt(); // 0=monster, 1=your hero, 2=opponent hero
      int x = in.nextInt(); // Position of this entity
      int y = in.nextInt();
      int shieldLife = in.nextInt(); // Ignore for this league; Count down until shield spell fades
      int isControlled = in.nextInt(); // Ignore for this league; Equals 1 when this entity is under a control spell
      int health = in.nextInt(); // Remaining health of this monster
      int vx = in.nextInt(); // Trajectory of this monster
      int vy = in.nextInt();
      int nearBase = in.nextInt(); // 0=monster with no target yet, 1=monster targeting a base
      int threatFor = in.nextInt(); // Given this monster's trajectory, is it a threat to 1=your base, 2=your
                                    // opponent's base, 0=neither

      if (type == 0) {
        map.addMonster(new Monster(id, x, y, shieldLife, isControlled, health, vx, vy, nearBase, threatFor));
      } else if (type == 1) {
        map.updateHero(id, x, y, shieldLife, isControlled);
      } else if (type == 2) {
        int idHero = id % 3;
        map.hisHeros[idHero] = new Hero(id, x, y, shieldLife, isControlled);
        map.spaces[map.hisHeros[idHero].pos.x][map.hisHeros[idHero].pos.y].nbHisHeros++;
      }
    }
  }
}

class Hero extends Entity {

  static final Strategies[] STRATEGIES = { Strategies.DEFENSIVE, Strategies.DEFENSIVE, Strategies.DEFENSIVE };

  static enum Strategies {
    DEFENSIVE
  }

  public void takeAction() {
    Player.map.bestActions[id].doAction();
  }

  // #region actions
  Strategies strategy = null;

  public Hero(int id, int x, int y, int shieldLife, int isControlled) {
    super(id, x, y, shieldLife, isControlled);
    if (id < 3) {
      this.strategy = STRATEGIES[id];
    }
  }

  public void waitHere() {
    System.out.println("WAIT");
  }

  public void waitHere(String message) {
    System.out.println("WAIT " + message);
  }

  @Override
  public String toString() {
    return "Hero " + id + " at " + pos;
  }

  // #endregion actions
}

// Class Space with myHeros, monsters, distanceToBase
class Space {
  Position pos;
  int nbMyHeros;
  int nbHisHeros;
  ArrayList<Monster> monsters;
  double distanceToBase;
  Score[] scoresParHeros = new Score[3];
  Action bestAction;
  boolean[] isDefensivePosition;

  public Space(Position pos) {
    this.pos = pos;
    distanceToBase = pos.getDistanceTo(Player.myBase.pos);

    isDefensivePosition = new boolean[] { Player.myBase.defensivePositions(0).equals(pos), //
        Player.myBase.defensivePositions(1).equals(pos), //
        Player.myBase.defensivePositions(2).equals(pos) };
    reset();
  }

  public void reset() {
    for (int i = 0; i < 3; i++) {
      scoresParHeros[i] = new Score();
    }
    bestAction = null;
    monsters = new ArrayList<>();
    nbMyHeros = 0;
    nbHisHeros = 0;
  }

  // Pour chaque heros, calculer le score
  public Score[] computeScores() {
    for (int i = 0; i < 3; i++) {
      Hero h = Player.map.heros[i];
      scoresParHeros[i].add(computeScore(h));
    }
    return scoresParHeros;
  }

  // Calculer le score pour le heros
  public Score computeScore(Hero hero) {
    switch (hero.strategy) {
      case DEFENSIVE:
        return computeScoreDefensive(hero);
      default:
        return computeScoreDefensive(hero);
    }
  }

  // Calculer le score pour le heros defensive
  public Score computeScoreDefensive(Hero hero) {
    final AtomicInteger score = new AtomicInteger(0);
    if (Player.myBase.isInDangerZone(pos)) {
      score.addAndGet(100);
    }
    int nbHerosDifferents = nbMyHeros;
    if (hero.pos.equals(pos)) {
      nbHerosDifferents--;
    }
    score.addAndGet(-nbHerosDifferents * 100);
    monsters.forEach(monster -> {
      score.addAndGet(50 + (int) (1000 / Player.myBase.pos.getDistanceTo(monster.pos)));
    });
    // TODO influencer les cases adjacentes
    if (isDefensivePosition[hero.id]) {
      score.addAndGet(50);
    }
    return new Score(score.get(), new Move(pos, score));
  }

}

// Class Map with base position and number of heroes
class Map {
  static final int RATIO = 100;
  static final int SIZE_X = Math.round(17630 / RATIO);
  static final int SIZE_Y = Math.round(9000 / RATIO);
  static final int DANGER_ZONE = 5000 / RATIO;

  ArrayList<Monster> monsters;
  ArrayList<Monster> monstersThreateningMe;
  Hero[] hisHeros;
  Hero[] heros;
  Space[][] spaces = new Space[SIZE_X][SIZE_Y];
  Action[] bestActions = new Action[3];

  Map() {
    this.monsters = new ArrayList<Monster>();
    this.monstersThreateningMe = new ArrayList<Monster>();
    this.hisHeros = new Hero[3];
    heros = new Hero[3];
    heros[0] = new Hero(0, 0, 0, 0, 0);
    heros[1] = new Hero(1, 0, 0, 0, 0);
    heros[2] = new Hero(2, 0, 0, 0, 0);

  }

  void initSpaces() {
    for (int i = 0; i < SIZE_X; i++) {
      for (int j = 0; j < SIZE_Y; j++) {
        spaces[i][j] = new Space(new Position(i, j, true));
      }
    }

  }

  // reset the map
  public void reset() {
    monsters.clear();
    monstersThreateningMe.clear();
    this.hisHeros = new Hero[3];

    for (int i = 0; i < SIZE_X; i++) {
      for (int j = 0; j < SIZE_Y; j++) {
        spaces[i][j].reset();
      }
    }
  }

  // update hero from id and x, y, shieldLife, isControlled
  public void updateHero(int id, int x, int y, int shieldLife, int isControlled) {
    heros[id].pos.to(x, y);
    heros[id].shieldLife = shieldLife;
    heros[id].isControlled = isControlled;
    spaces[heros[id].pos.x][heros[id].pos.y].nbMyHeros++;

  }

  public void addMonster(Monster monster) {
    monsters.add(monster);
    if (monster.isThreatForMe()) {
      monstersThreateningMe.add(monster);
    }
    spaces[monster.pos.x][monster.pos.y].monsters.add(monster);
  }

  // Compute score for each space and find the best action for each hero
  public void computeScores() {
    Score[] bestScores = new Score[] { Score.MIN, Score.MIN, Score.MIN };
    for (int i = 0; i < SIZE_X; i++) {
      for (int j = 0; j < SIZE_Y; j++) {
        Score[] scores = spaces[i][j].computeScores();
        for (int k = 0; k < 3; k++) {
          if (scores[k].score > bestScores[k].score) {
            bestScores[k] = scores[k];
          }
        }
      }
    }
    for (int i = 0; i < 3; i++) {
      bestActions[i] = bestScores[i].action;
    }
  }

  // Return true if there is no monster threatening me
  public boolean hasNoMonsterThreatening() {
    return monstersThreateningMe.size() == 0;
  }
}

// ##################
// DUMMY CLASSES
// ##################

// Class Position with x and y
class Position {
  int x;
  int y;

  Position(int x, int y) {
    this(x, y, false);
  }

  Position(int x, int y, boolean absolute) {
    if (absolute) {
      this.x = x;
      this.y = y;
    } else {
      to(x, y);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Position) {
      Position p = (Position) obj;
      return p.x == this.x && p.y == this.y;
    }
    return false;
  }

  Position toReal() {
    return new Position(x * Map.RATIO, y * Map.RATIO, true);
  }

  void to(int x, int y) {
    this.x = Math.max(0, Math.min(Map.SIZE_X, x / Map.RATIO));
    this.y = Math.max(0, Math.min(Map.SIZE_Y, y / Map.RATIO));
  }

  // Return the distance to the given position
  public double getDistanceTo(Position p) {
    return Math.sqrt(Math.pow(x - p.x, 2) + Math.pow(y - p.y, 2));
  }

  // return the current position limited to a given radius around a given position
  public Position getPositionLimitedToRadius(Position p, int radius) {
    double distance = getDistanceTo(p);
    if (distance > radius) {
      double ratio = radius / distance;
      return new Position((int) (x * ratio), (int) (y * ratio));
    } else {
      return this;
    }
  }

  // Static method to generate a random position in a given radius around a given
  // position
  public static Position getRandomPositionLimitedToRadius(Position p, double d) {
    Position pos = new Position(0, 0);
    double angle = Math.random() * 2 * Math.PI;
    pos.x = (int) (p.x + d * Math.cos(angle));
    pos.y = (int) (p.y + d * Math.sin(angle));
    return pos;
  }

  @Override
  public String toString() {
    return "(" + x + "," + y + ")";
  }
}

// Class Base with health and mana
class Base {
  Position pos;
  int health;
  int mana;
  static final int[][][] DEFENSIVE_POSITIONS = new int[][][] {
      {},
      { { 10, 10 } },
      { { 30, 10 }, { 10, 30 } },
      { { 40, 20 }, { 10, 10 }, { 20, 40 } }
  };

  Base(int x, int y) {
    pos = new Position(x, y);
    this.health = 0;
    this.mana = 0;
  }

  // Compute defensive positions
  Position defensivePositions(int id) {
    int countDefenseur = 0;
    for (int i = 0; i < 3; i++) {
      if (Hero.Strategies.DEFENSIVE.equals(Player.map.heros[i].strategy)) {
        countDefenseur++;
      }
    }
    if (pos.x == 0) {
      int[] posDef = DEFENSIVE_POSITIONS[countDefenseur][id];
      return new Position(posDef[0], posDef[1], true);
    } else {
      int[] posDef = DEFENSIVE_POSITIONS[countDefenseur][id];
      return new Position(pos.x - posDef[0], pos.y - posDef[1], true);
    }
  }

  // is in danger zone
  public boolean isInDangerZone(Position p) {
    return pos.getDistanceTo(p) < Map.DANGER_ZONE;
  }
}

// Class monster with id, x, y, shieldLife, isControlled, health, vx, vy,
// nearBase, and threatFor
class Monster extends Entity {
  int health;
  int vx;
  int vy;
  int nearBase;
  int threatFor;

  public Monster(int id, int x, int y, int shieldLife, int isControlled, int health, int vx, int vy, int nearBase,
      int threatFor) {
    super(id, x, y, shieldLife, isControlled);
    this.health = health;
    this.vx = vx;
    this.vy = vy;
    this.nearBase = nearBase;
    this.threatFor = threatFor;
  }

  public boolean isThreatForMe() {
    return threatFor == 1 && Player.myBase.isInDangerZone(pos);
  }

  // toString id and pos
  @Override
  public String toString() {
    return "Monster " + id + " " + pos;
  }
}

// Class Entity with id, x, y, shieldLife, isControlled
class Entity {
  Position pos;
  int id;
  int shieldLife;
  int isControlled;

  public Entity(int id, int x, int y, int shieldLife, int isControlled) {
    this.id = id;
    pos = new Position(x, y);
    this.shieldLife = shieldLife;
    this.isControlled = isControlled;
  }

  // Get the distance to the given entity
  public double getDistanceTo(Entity e) {
    return pos.getDistanceTo(e.pos);
  }
}

// Class Score with score and action
class Score {
  static final Score MIN = new Score(Integer.MIN_VALUE, null);
  double score;
  Action action;

  Score() {
    this.score = 0;
    this.action = null;
  }

  Score(double score, Action action) {
    this.score = score;
    this.action = action;
  }

  void add(Score score) {
    this.score += score.score;
    if (score.action != null) {
      this.action = score.action;
    }
  }
}

// abstract Class Action with verb
abstract class Action {
  String action;
  Object message;

  Action() {
    this.action = "";
    this.message = "";
  }

  Action(Object message) {
    this.message = message;
  }

  void doAction() {
    System.out.println(action + " " + message);
  }

  @Override
  public String toString() {
    return action;
  }
}

// Class Move with position
class Move extends Action {
  Move(Position pos, Object message) {
    super(message);
    Position finalPos = pos.toReal();
    this.action = "MOVE " + finalPos.x + " " + finalPos.y;
  }

  Move(Position pos) {
    this(pos, "");
  }
}

// Class Wind
class Wind extends Action {
  Wind(Position pos, Object message) {
    super(message);
    Position finalPos = pos.toReal();
    this.action = "WIND " + finalPos.x + " " + finalPos.y;
  }

  Wind(Position pos) {
    this(pos, "");
  }
}

// Class Shield with id
class Shield extends Action {
  Shield(int id, Object message) {
    super(message);
    this.action = "SHIELD " + id;
  }

  Shield(int id) {
    this(id, "");
  }
}

// Class Control with id and position
class Control extends Action {
  Control(int id, Position pos, Object message) {
    super(message);
    Position finalPos = pos.toReal();
    this.action = "CONTROL " + id + " " + finalPos.x + " " + finalPos.y;
  }

  Control(int id, Position pos) {
    this(id, pos, "");
  }
}
