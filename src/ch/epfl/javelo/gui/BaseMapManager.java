package ch.epfl.javelo.gui;

import ch.epfl.javelo.Math2;
import ch.epfl.javelo.projection.PointWebMercator;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


public final class BaseMapManager {

    private final int TILE_PIXEL_SIZE = 256;
    private final TileManager tm;
    private final WaypointsManager wm;
    private final ObjectProperty<MapViewParameters> mvp;

    private final Pane pane;
    private final GraphicsContext graphContext;

    private boolean redrawNeeded;



    public BaseMapManager(TileManager tm, WaypointsManager wm, ObjectProperty<MapViewParameters> mvp) {

        this.tm = tm;
        this.wm = wm;
        this.mvp = mvp;

        Canvas canvas = new Canvas();
        pane = new Pane(canvas);

        paneEvent();
        pane.setPickOnBounds(false);

        graphContext = canvas.getGraphicsContext2D();


        canvas.widthProperty().bind(pane.widthProperty());
        canvas.heightProperty().bind(pane.heightProperty());

        canvas.widthProperty().addListener(o -> redrawOnNextPulse());
        canvas.heightProperty().addListener(o -> redrawOnNextPulse());

        canvas.sceneProperty().addListener((p, oldS, newS) -> {
            assert oldS == null;
            newS.addPreLayoutPulseListener(this::redrawIfNeeded);
        });

        redrawOnNextPulse();
    }

    public Pane pane() {
        return pane;
    }

    private void draw() {
        double x = mvp.get().topLeft().getX();
        double y = mvp.get().topLeft().getY();
        int z = mvp.get().zoomLevel();

        for (int i = 0; i < pane.getWidth() + TILE_PIXEL_SIZE; i += TILE_PIXEL_SIZE) {
            for (int j = 0; j < pane.getHeight() + TILE_PIXEL_SIZE; j += TILE_PIXEL_SIZE) {
                try {
                    TileManager.TileId ti = new TileManager.TileId(z,
                            (int) Math.floor((i + x) / TILE_PIXEL_SIZE),
                            (int) Math.floor((y + j)/ TILE_PIXEL_SIZE));
                    graphContext.drawImage(tm.imageForTileAt(ti), i - x % TILE_PIXEL_SIZE, j - y % TILE_PIXEL_SIZE);

                } catch (IOException | IllegalArgumentException ignored) {
                }

            }
        }
    }

    private void paneEvent(){

        ObjectProperty<Point2D> dragged = new SimpleObjectProperty<>();

        SimpleLongProperty minScrollTime = new SimpleLongProperty();
        pane.setOnScroll(e -> {
            if (e.getDeltaY() == 0d) return;
            long currentTime = System.currentTimeMillis();
            if (currentTime < minScrollTime.get()) return;
            minScrollTime.set(currentTime + 200);
            int zoomDelta = (int) Math.signum(e.getDeltaY());
            int oldZ = mvp.get().zoomLevel();

            PointWebMercator temp = mvp.get().pointAt((int) e.getX(),(int) e.getY());
            int newX = (int) (temp.xAtZoomLevel(oldZ + zoomDelta)-e.getX());
            int newY = (int) (temp.yAtZoomLevel(oldZ + zoomDelta)-e.getY());
            mvp.set(new MapViewParameters(oldZ + zoomDelta, newX, newY));
            redrawOnNextPulse();
        });

        pane.setOnMousePressed(event -> {
            dragged.set(new Point2D(event.getX(), event.getY()));
        });

        pane.setOnMouseDragged(event -> {
            int diffX = (int) (event.getX()-dragged.get().getX());
            int diffY = (int) (event.getY()-dragged.get().getY());
            mvp.set(new MapViewParameters(mvp.get().zoomLevel(),mvp.get().topLeft().getX() - diffX, mvp.get().topLeft().getY() - diffY));
            dragged.set(new Point2D(event.getX(), event.getY()));
            redrawOnNextPulse();

        });

        pane.setOnMouseReleased(event -> {
            if(event.isStillSincePress()) {
                wm.addWaypoint((int) event.getX(), (int) event.getY());
                redrawOnNextPulse();
            }

        });
    }

    private void redrawIfNeeded() {
        if (!redrawNeeded) return;
        redrawNeeded = false;
        draw();
    }


    private void redrawOnNextPulse() {
        redrawNeeded = true;
        Platform.requestNextPulse();
    }
}