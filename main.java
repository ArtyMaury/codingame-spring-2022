import java.util.ArrayList;
import java.util.Scanner;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
  static Map map;
  static Base myBase;

  static void debug(Object obj) {
    System.err.println(obj);
  }

  public static void main(String args[]) {
    Scanner in = new Scanner(System.in);
    myBase = new Base(in.nextInt(), in.nextInt());
    map = new Map(in.nextInt());

    // game loop
    while (true) {
      init(in);

      debug(map.myHeros);
      if (map.hasNoMonsterThreatening()) {
        map.spreadMyHeros();
      } else {
        for (Hero myHero : map.myHeros) {
          Monster nearestMonster = map.getNearestMonster(myHero);
          // debug(myHero + " vise " + nearestMonster);
          myHero.moveTo(nearestMonster.pos.getPositionLimitedToRadius(myBase.pos, Base.DANGER_ZONE));
        }
      }

    }
  }

  private static void init(Scanner in) {
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
        map.myHeros.add(new Hero(id, x, y, shieldLife, isControlled));
      } else if (type == 2) {
        map.hisHeros.add(new Hero(id, x, y, shieldLife, isControlled));
      }
    }
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
    return threatFor == 1;
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

class Hero extends Entity {
  public Hero(int id, int x, int y, int shieldLife, int isControlled) {
    super(id, x, y, shieldLife, isControlled);
  }

  // Move to the given monster
  public void moveTo(Monster m) {
    Player.debug("Moving to " + m);
    moveTo(m.pos);
  }

  // print the given position
  public void moveTo(Position pos) {
    System.out.println("MOVE " + pos.x + " " + pos.y);
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
}

// Class Map with base position and number of heroes
class Map {
  int heroesPerPlayer;
  ArrayList<Monster> monstersThreateningMe;
  ArrayList<Monster> monstersThreateningHim;
  ArrayList<Hero> myHeros;
  ArrayList<Hero> hisHeros;

  Map(int heroesPerPlayer) {
    this.heroesPerPlayer = heroesPerPlayer;
    this.monstersThreateningMe = new ArrayList<Monster>();
    this.monstersThreateningHim = new ArrayList<Monster>();
    this.myHeros = new ArrayList<Hero>();
    this.hisHeros = new ArrayList<Hero>();
  }

  public void addMonster(Monster monster) {
    if (monster.isThreatForMe()) {
      monstersThreateningMe.add(monster);
    } else {
      monstersThreateningHim.add(monster);
    }
  }

  // Return the nearest monster to the hero
  public Monster getNearestMonster(Hero hero) {
    Monster nearestMonster = null;
    double minDistance = Integer.MAX_VALUE;
    for (Monster monster : monstersThreateningMe) {
      double distance = hero.getDistanceTo(monster);
      if (distance < minDistance) {
        minDistance = distance;
        nearestMonster = monster;
      }
    }
    return nearestMonster;
  }

  // Return true if there is no monster threatening me
  public boolean hasNoMonsterThreatening() {
    return monstersThreateningMe.size() == 0;
  }

  // Spread the heroes to in a Base.DANGER_ZONE radius
  public void spreadMyHeros() {
    for (Hero hero : myHeros) {
      hero.moveTo(Position.getRandomPositionLimitedToRadius(Player.myBase.pos, Base.DANGER_ZONE));
    }
  }
}

// Class Base with health and mana
class Base {
  static final int DANGER_ZONE = 5000;
  Position pos;
  int health;
  int mana;

  Base(int x, int y) {
    pos = new Position(x, y);
    this.health = 0;
    this.mana = 0;
  }

  // Return the distance to the given entity
  public double getDistanceTo(Entity e) {
    return pos.getDistanceTo(e.pos);
  }
}

// Class Position with x and y
class Position {
  int x;
  int y;

  Position(int x, int y) {
    this.x = Math.max(0, Math.min(17630, x));
    this.y = Math.max(0, Math.min(9000, y));
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
  public static Position getRandomPositionLimitedToRadius(Position p, int radius) {
    Position pos = new Position(0, 0);
    double angle = Math.random() * 2 * Math.PI;
    pos.x = (int) (p.x + radius * Math.cos(angle));
    pos.y = (int) (p.y + radius * Math.sin(angle));
    return pos;
  }

  @Override
  public String toString() {
    return "(" + x + "," + y + ")";
  }
}
