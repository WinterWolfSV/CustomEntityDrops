package winterwolfsv.custom_entity_drops;

public class DropSaver {
    public String UUID;
    public String item;
    public int amount;

    public DropSaver(String UUID, String item, int amount) {
        this.UUID = UUID;
        this.item = item;
        this.amount = amount;
    }
}
