package ch.epfl.javelo.routing;

import ch.epfl.javelo.Preconditions;
import ch.epfl.javelo.data.Graph;

import java.util.*;

public final class RouteComputer {

    private final Graph graph;
    private final CostFunction costFunction;

    public RouteComputer(Graph graph, CostFunction costFunction){
        this.graph = graph;
        this.costFunction = costFunction;
    }

    public Route bestRouteBetween(int startNodeId, int endNodeId){

        Preconditions.checkArgument(startNodeId!=endNodeId);

        int numberNodes = graph.nodeCount();
        double[] distance = new double[numberNodes];
        int[] predecessor = new int[numberNodes];

        List<Integer> nodePath = new ArrayList<>();
        Set<Integer> set = new HashSet<>();
        List<Edge> edges = new ArrayList<>();


        Arrays.fill(distance,Double.POSITIVE_INFINITY);
        distance[startNodeId] = 0;
        set.add(startNodeId);
        nodePath.add(endNodeId);

        do{
            final int N = smallestDistance(set,distance);
            set.removeIf(x -> (x==N));
            if(N == endNodeId){break;}
            int quantity = graph.nodeOutDegree(N);
            for (int i = 0; i < quantity; i++) {
                int NP = graph.edgeTargetNodeId(graph.nodeOutEdgeId(N,i));
                double d = distance[N] + costFunction.costFactor(N,graph.nodeOutEdgeId(N,i))*graph.edgeLength(graph.nodeOutEdgeId(N,i));
                if(d<distance[NP]){
                    distance[NP] = d;
                    predecessor[NP] = N;
                    set.add(NP);
                }
            }
        }while(!set.isEmpty());
        int i = endNodeId;
        while(i != 0){
            i = predecessor[i];
            nodePath.add(i);
        }
        Collections.reverse(nodePath);
        for (int j = 1; j < nodePath.size()-1; j++) {
            boolean found = false;
            int h = 0;
            int index = graph.nodeOutEdgeId(nodePath.get(j),h);
            while(!found){
                if(graph.edgeTargetNodeId(index) == nodePath.get(j+1)){
                    edges.add(Edge.of(graph, index,nodePath.get(j),  nodePath.get(j + 1)));
                    found = true;
                }
                h++;
            }

        }
        return new SingleRoute(edges);
    }

    private Integer smallestDistance(Set<Integer> set, double[] distance){
        double min = Double.POSITIVE_INFINITY;
        int N = 0;
        for(Integer i : set){
            if(distance[i]<min){
                min = distance[i];
                N = i;
            }
        }
        return N;
    }
}
