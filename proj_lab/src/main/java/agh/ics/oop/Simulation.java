package agh.ics.oop;

import agh.ics.oop.model.*;
import agh.ics.oop.model.elements.*;
import agh.ics.oop.model.maps.*;
import agh.ics.oop.model.util.RandomPositionsGenerator;

import java.io.FileNotFoundException;
import java.util.*;


public class Simulation implements Runnable{
    private final ArrayList<Animal> animalsAlive = new ArrayList<>(0);
    private final List<Animal> animalsDead = new ArrayList<>();
    private final Map<Genome,List<Animal>> genomeList = new HashMap<>();
    private final WorldMap map;
    private final SimulationParameters simulationParameters;
    private final RandomPositionsGenerator randomPositionsGenerator = new RandomPositionsGenerator();
    private boolean isRunning = false;
    private int dayCounter = 1;

    public Simulation(SimulationParameters simulationParameters) throws FileNotFoundException {
        this.simulationParameters = simulationParameters;
        map = simulationParameters.getMap();


        placeAnimals();

        ArrayList<Vector2d> grassPositions = randomPositionsGenerator.kPositionsNoRepetition
                (map.getMapBorders().allPositions(), simulationParameters.startingGrassAmount());

        for(Vector2d position: grassPositions) { //rozkladanie trawy na mapie
            Grass grass = new Grass(position);
            this.map.placeGrass(grass);
        }
    }

    public void placeAnimals(){
        ArrayList<Vector2d> animalPositions = randomPositionsGenerator.kPositionsWithRepetition
                (map.getMapBorders().allPositions(), simulationParameters.startingAnimalAmount());

        for(Vector2d position: animalPositions){
            Animal animal = new Animal(simulationParameters.getGenome(),
                    position,
                    simulationParameters.energyParameters().startingEnergy());
            this.map.placeAnimal(animal);
            animalsAlive.add(animal);
            addGenome(animal);
        }
    }
    public Animal getAnimal(int i) {
        assert i>=0 && i<animalsAlive.size();
        return animalsAlive.get(i);
    }

    public WorldMap getMap() {
        return map;
    }

    public void removeIfDead(Animal animal) {
        if (animal.getEnergy()<=0){
            this.animalsAlive.remove(animal);
            map.removeAnimal(animal);
            animalsDead.add(animal);
            animal.setDeathDay(dayCounter);
            System.out.println("Zwierze umarlo na pozycji "+animal.getPosition()+". Żyło "+animal.getLifeSpan());
        }
    }
    public void reproduce(Vector2d position){
        if(map.animalsAt(position).size()>=2){
            List<Animal> parents = map.kWinners(position,2);
            if(parents.get(1).getEnergy()>=simulationParameters.energyParameters().energyToFull()){
                Animal child = new Animal(parents.get(0),parents.get(1),simulationParameters);
                map.placeAnimal(child);
                animalsAlive.add(child);

                addGenome(child);
                System.out.println("Nowe zwierze na pozycji: "+child.getPosition()+", "+child.getGenome());
            }
        }
    }
    public void addGenome(Animal animal){
        Genome genome = animal.getGenome();
        if (!genomeList.containsKey(genome)){
            genomeList.put(genome,new ArrayList<>());
        }
        genomeList.get(genome).add(animal);
    }

    public Genome mostPopularGenome() {
        Genome resGenome = new DefaultGenome(1);
        int currMaxSize = 0;
        for(Genome genome : genomeList.keySet() ){
            if (genomeList.get(genome).size() > currMaxSize) {
                currMaxSize = genomeList.get(genome).size();
                resGenome = genome;
            }
        }
        return resGenome;
    }

    public int averageEnergy() {
        if (animalsAlive.isEmpty()) return 0;
        return animalsAlive.stream().mapToInt(Animal::getEnergy).sum()/animalsAlive.size();
    }


    public int averageLifeSpan() {
        if (animalsDead.isEmpty()) return 0;
        return animalsDead.stream().mapToInt(Animal::getLifeSpan).sum()/animalsDead.size();
    }

    public float averageChildrenNumber() {
        return (float) animalsAlive.stream()
                .mapToInt(animal -> animal.getChildren().size())
                .sum() /animalsAlive.size();
    }

    public void printStatistics(){
        System.out.println("Średnia energia wśród żyjących zwierząt: "+averageEnergy());
        System.out.println("Średnia liczba dzieci wśród żyjących zwierząt: "+averageChildrenNumber());
        System.out.println("Liczba zmarłych zwierząt: "+animalsDead.size());
        System.out.println("Średnia długość życia wśród zmarłych zwierząt: "+averageLifeSpan() );
        System.out.println("Najpopularniejszy genom dotychczas: "+mostPopularGenome());
        System.out.println("Zwierzeta z tym genomem: "+genomeList.get(mostPopularGenome()));
    }
    public void startRunning(){
        this.isRunning = true;
    }
    public void stopRunning(){
        this.isRunning = false;
    }
    @Override
    public void run() {
        startRunning();
        while(true) { // na razie daje tu losowa liczbe jako liczbe wykonan


            System.out.println("Dzień: "+this.dayCounter);
            if(this.dayCounter%25 == 0) printStatistics();

            //usuniecie martwych zwierzakow
            new ArrayList<>(animalsAlive).forEach(this::removeIfDead);

            //ruszanie sie zwierzakow
            animalsAlive.forEach(map::move);

            //jedzenie roslin
            new ArrayList<>(map.getGrasses().values()).forEach(map::eatGrass);

            //rozmnazanie sie najedzonych zwierzakow
            map.getMapBorders().allPositions().forEach(this::reproduce); // do sprawdzenia

            //nowe rosliny
            map.growGrass(simulationParameters.dailyGrassGrowth());

            this.dayCounter+=1;

                try {
                    if (isRunning) {
                        Thread.sleep(700); // to jest niepewne
                    }
                    else{
                        while (!isRunning){
                            Thread.sleep(100);
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
