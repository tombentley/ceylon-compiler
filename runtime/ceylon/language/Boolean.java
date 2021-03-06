package ceylon.language;

public abstract class Boolean extends Case {

    public Boolean(java.lang.String caseName) {
        super(caseName);
    }

    public static Boolean instance(boolean b) {
        return b ? $true.getTrue() : $false.getFalse();
    }

    abstract public boolean booleanValue();

}
