package netherfreedom.utils;

public class ServiceLoader {

    public static void load() {
    OSUtils.init(); // setup current os for stuff like spotify
    SpotifyService.init();
    Runtime.getRuntime().addShutdownHook(new Thread(ThreadLoader::shutdown));
    }
}

