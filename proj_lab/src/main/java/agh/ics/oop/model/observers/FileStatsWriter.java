package agh.ics.oop.model.observers;

import agh.ics.oop.Simulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class FileStatsWriter implements MapChangeListener {
    private final PrintWriter pw;
    public FileStatsWriter(String mapName) {
        File file = new File(mapName + ".csv");
        try {
            this.pw = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void mapChanged(Simulation simulation, String message) {
        pw.print(simulation.getAliveAnimalsCount());
        pw.print(","+simulation.getMap().countGrass());
        pw.print(","+simulation.getMap().emptyPositions().size());
        pw.print(","+simulation.getMostPopularGenome().toString());
        pw.print(","+simulation.getAverageEnergy());
        pw.print(","+simulation.getAverageLifespan());
        pw.println(","+(simulation.getAverageChildrenCount()));
        pw.flush();

    }
}
