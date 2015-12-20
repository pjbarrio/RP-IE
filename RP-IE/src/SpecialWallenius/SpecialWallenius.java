package SpecialWallenius;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Stack;

import edu.columbia.cs.utils.Pair;

public class SpecialWallenius {

	private int N;
	private int m;
	private WeightsFunction w;

	public SpecialWallenius(int N, int m, WeightsFunction w){
		this.N=N;
		this.m=m;
		this.w=w;
	}

	public double getExpectedValue(int n){
		HashMap<Pair<Integer,Integer>,Double> cache = new HashMap<Pair<Integer,Integer>,Double>();
		double res = 0;
		for(int x=1; x<=n; x++){
			System.out.println(x);
			double prob = getProbAux(x, n, m, N-m, cache);
			res+=prob*x;
		}
		return res;
	}

	public double getProbability(int x, int n){
		if(x>m){
			//Cannot get more positive balls than what we can find in the urn
			return 0;
		}

		return getProbAux(x, n, m, N-m, new HashMap<Pair<Integer,Integer>,Double>());
	}

	private double getProbAux(int x, int n, int m1, int m2, HashMap<Pair<Integer, Integer>, Double> cache){

		Pair<Integer,Integer> objective = new Pair<Integer, Integer>(x, n);

		Stack<Pair<Integer,Integer>> stack = new Stack<Pair<Integer,Integer>>();
		stack.push(objective);

		while(!stack.isEmpty()){
			Pair<Integer,Integer> top = stack.pop();

			if(cache.containsKey(top)){
				System.out.println("Entrei!!!");
				continue;
			}

			x=top.first();
			n=top.second();

			if(x<Math.max(0, n-m2)){
				cache.put(top, 0.0);
				continue;
			}
			if(x>Math.min(n, m1)){
				cache.put(top, 0.0);
				continue;
			}

			if(n==1){
				if(x==1){
					cache.put(top, (m1*w.getWeights(n)/(m1*w.getWeights(n)+m2)));
					continue;
				}else if(x==0){
					cache.put(top, (m2/(m1*w.getWeights(n)+m2)));
					continue;
				}else{
					cache.put(top, 0.0);
					continue;
				}
			}

			Pair<Integer,Integer> c1 = new Pair<Integer, Integer>(x-1, n-1);
			Pair<Integer,Integer> c2 = new Pair<Integer, Integer>(x, n-1);

			Double v1 = cache.get(c1);
			Double v2 = cache.get(c2);

			if(v1==null && v2==null){
				stack.push(top);
				stack.push(c1);
				stack.push(c2);
			}else if(v1==null){
				stack.push(top);
				stack.push(c1);
			}else if(v2==null){
				stack.push(top);
				stack.push(c2);
			}else{
				//System.out.println("Computing " + top);
				cache.put(top, v1*((m1-x+1)*w.getWeights(n)/((m1-x+1)*w.getWeights(n)+m2+x-n)) +
						v2*((m2+x-n+1)/((m1-x)*w.getWeights(n)+m2+x-n+1)));
			}
		}

		return cache.get(objective);
	}
	
	public double getRecallNewtonMethod(int n, int N, int m1, int iter){
		double minResult = Math.max(0, n-(N-m1));
		double maxResult = Math.min(n, m1);
		double result = (maxResult+minResult)/2.0;
				
		double k1 = w.getLimit()/(N-m1);
		double k2 = 1.0/m1;
		for(int i=0; i<iter; i++){
			double coefExp = 1.0-(n-result)/(N-m1);
			double num = Math.pow(coefExp, w.getLimit())-1+(result/m1);
			double den = k1*Math.pow(coefExp, w.getLimit()-1)+k2;
			double newResult = result - num/den;
			if(result==newResult){
				//If the result did not change it means that we will not make more progress.
				break;
			}
			result = newResult;
		}
				
		return result;
	}

	public static void main(String[] args) {
		int N = 4;
		int m1 = 2;
		SpecialWallenius p = new SpecialWallenius(N, m1, new WeightsFunction());
		for(int n=0; n<=4; n++){
			System.out.println("For " + n);
			double sum =0;
			for(int x=0; x<=n; x++){
				double prob = p.getProbability(x, n);
				System.out.println(x + ": " + prob);
				sum+=+prob;
			}
			System.out.println("Sum: " + sum);
			System.out.println("Expected: " + p.getExpectedValue(n));
			System.out.println("Predicted: " + p.getRecallNewtonMethod(n, N, m1, 100));
		}
	}
}
