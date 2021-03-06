package ceylon.language;
import java.lang.reflect.Method;

public class Launcher
{
    public static void main(java.lang.String[] args)
        throws Throwable
    {
        Class<?> c = Class.forName(args[0]);

        Method m = c.getMethod(c.getSimpleName(), ceylon.language.Process.class);
        m.invoke(null, new Process());
    }
}
