package ceylon.language;

@Extension
public class ObjectLog extends ceylon.language.Object {
    private ceylon.language.Object object;

    public ObjectLog(ceylon.language.Object object) {
        this.object = object;
    }

    public Log log() {
        return null; // XXX
    }
}
