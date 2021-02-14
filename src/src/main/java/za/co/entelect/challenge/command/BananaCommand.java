package za.co.entelect.challenge.command;

public class BananaCommand implements Command {
    private final x;
    private final y;

    public BananaCommand(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String render() {
        return String.format("banana %d %d", x, y);
    }
}