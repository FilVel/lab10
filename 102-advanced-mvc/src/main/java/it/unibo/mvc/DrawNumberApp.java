package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final int MIN = 0;
    private static final int MAX = 100;
    private static final int ATTEMPTS = 10;

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final String configurationFile, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        Map<String, Integer> config = new HashMap<String, Integer>();
        try (var configurations = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(configurationFile)))) {
            for (var configuration = configurations.readLine(); configuration != null; configuration = configurations.readLine()) {
                final String[] lineElements = configuration.split(":");
                if (lineElements.length == 2) {
                    final int value = Integer.parseInt(lineElements[1].trim());
                    if (lineElements[0].contains("max")) {
                        config.put("max", value);
                    } else if (lineElements[0].contains("min")) {
                        config.put("min", value);
                    } else if (lineElements[0].contains("attempts")) {
                        config.put("attemps", value);
                    }
                } else {
                    for (final DrawNumberView view : this.views) {
                        view.displayError("I can not understand \"" + configuration + '"');                        
                    }
                }
            }
        } catch (IOException e) {
            for (final DrawNumberView view : this.views) {
                view.displayError(e.getMessage());                        
            }
        }
        if ((config.get("max") <= config.get("min")) || (config.get("attempts") <= 0)){
            for (final DrawNumberView view : this.views) {
                view.displayError("Incompatible configuration, using deafult one this time");
                System.out.println(config.get("max"));                 
            }
            this.model = new DrawNumberImpl(MIN, MAX, ATTEMPTS);
        } else {
            this.model = new DrawNumberImpl(config.get("min"), config.get("max"), config.get("attempts"));
        }
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp("src/main/resources/config.yml", new DrawNumberViewImpl());
    }

}
