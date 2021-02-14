package za.co.entelect.challenge.command;

public class SelectCommand implements Command {
    private final int x;
    private final int y;
    private final int id;
    private final string command

    public SelectCommand(int x, int y, int id, string command) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.command = command;
    }

    @Override
    public String render() {
        return String.format("select %d; %s %d %d", id, command, x, y);
    }
}