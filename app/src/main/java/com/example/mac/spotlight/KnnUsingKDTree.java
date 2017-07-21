package com.example.mac.spotlight;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

class DistanceCalculator
{
    public static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        }

        return (dist);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
	/*::	This function converts decimal degrees to radians						 :*/
	/*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
	/*::	This function converts radians to decimal degrees						 :*/
	/*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
}
class KDNode
{
    int axis;
    double[] x;
    int id;
    boolean checked;
    boolean orientation;


    KDNode Parent;
    KDNode Left;
    KDNode Right;

    public KDNode(double[] x0, int axis0)
    {
        x = new double[7];
        axis = axis0;
        for (int k = 0; k < 7; k++)
            x[k] = x0[k];

        Left = Right = Parent = null;
        checked = false;
        id = 0;
    }

    public KDNode FindParent(double[] x0)
    {
        KDNode parent = null;
        KDNode next = this;
        int split;
        while (next != null)
        {
            split = next.axis;
            parent = next;
            if (x0[split] > next.x[split])
                next = next.Right;
            else
                next = next.Left;
        }
        return parent;
    }

    public KDNode Insert(double[] p)
    {
        //x = new double[2];
        KDNode parent = FindParent(p);
        if (equal(p, parent.x, 4) == true)
            return null;

        KDNode newNode = new KDNode(p, parent.axis + 1 < 6 ? parent.axis + 1
                : 0);
        newNode.Parent = parent;

        if (p[parent.axis] > parent.x[parent.axis])
        {
            parent.Right = newNode;
            newNode.orientation = true; //
        } else
        {
            parent.Left = newNode;
            newNode.orientation = false; //
        }

        return newNode;
    }



    boolean equal(double[] x1, double[] x2, int dim)
    {
        if(x1[6]!=x2[6])
            return false;
        for (int k = 0; k < 3; k++)
        {
            if (x1[k] != x2[k])
                return false;
        }
        DistanceCalculator d = new DistanceCalculator();
        double d1 = d.distance(x1[4], x1[5], x2[4], x2[5], "M");
        if(d1>1)
            return false;

        //distance
        // TODO

        return true;
    }

    double distance2(double[] x1, double[] x2, int dim)
    {
        double S = 0;
        //for (int k = 0; k < dim; k++)
        //S += (x1[k] - x2[k]) * (x1[k] - x2[k]);
        //TODO update s as required
        S += Math.abs(x1[0]-x2[0])/5;//time slot
        if(x1[1]==50){
            S+=1;
        }
        else{
            S += Math.abs(x1[1]-x2[1])/100;//headphone
        }
        if(x1[2]==50){
            S+=1;
        }
        else{
            S += Math.abs(x1[2]-x2[2])/100;//user_activity

        }

        S += Math.abs(7-x2[3])/7.5;//frequency+recently_used
        //Log.i("FREQ+REC",Double.toString(x2[3]));
        //distance
        // TODO
        if(x1[4]==0||x1[5]==0){
            S+=1;
        }
        else {
            DistanceCalculator d = new DistanceCalculator();
            double d1 = d.distance(x1[4], x1[5], x2[4], x2[5], "M");
            if (d1 < 1)
                S += 0;
            else if (d1 < 5)
                S += .25;
            else if (d1 < 10)
                S += .50;
            else if (d1 < 50)
                S += .75;
            else
                S += 1.00;
        }

        //Log.e("SSS",Double.toString(S));
        return S;
    }
}

class KDTree
{
    KDNode Root;

    int TimeStart, TimeFinish;
    int CounterFreq;

    double d_min;
    KDNode nearest_neighbour;

    int KD_id;

    int nList;

    KDNode CheckedNodes[];
    int checked_nodes;
    KDNode List[];

    double x_min[], x_max[];
    boolean max_boundary[], min_boundary[];
    int n_boundary;

    public KDTree(int i)
    {
        Root = null;
        KD_id = 1;
        nList = 0;
        List = new KDNode[i];
        CheckedNodes = new KDNode[i];
        max_boundary = new boolean[6];
        min_boundary = new boolean[6];
        x_min = new double[6];
        x_max = new double[6];
    }

    public boolean add(double[] x)
    {
        if (nList >= 2000000 - 1)
            return false; // can't add more points

        if (Root == null)
        {
            Root = new KDNode(x, 0);
            Root.id = KD_id++;
            List[nList++] = Root;
        } else
        {
            KDNode pNode;
            if ((pNode = Root.Insert(x)) != null)
            {
                pNode.id = KD_id++;
                List[nList++] = pNode;
            }
        }

        return true;
    }

    public KDNode find_nearest(double[] x)
    {
        if (Root == null)
            return null;

        checked_nodes = 0;
        KDNode parent = Root.FindParent(x);
        nearest_neighbour = parent;
        d_min = Root.distance2(x, parent.x, 4);
        ;

        search_parent(parent, x);
        uncheck();
        Log.e("DISTANCE",Double.toString(d_min));
        return nearest_neighbour;
    }
    //TODO START
    public void check_subtree(KDNode node, double[] x)
    {
        if ((node == null) || node.checked)
            return;

        CheckedNodes[checked_nodes++] = node;
        node.checked = true;
        set_bounding_cube(node, x);
        double d;
        int dim = node.axis;
        if(dim==0)
            d= Math.abs(node.x[dim] - x[dim])/5;
        else if(dim==1)
            d= Math.abs(node.x[dim] - x[dim])/100;
        else if(dim==2)
            d= Math.abs(node.x[dim] - x[dim])/100;
        else if(dim==3)
            d= Math.abs(node.x[dim] - x[dim])/7.5;
        else if(dim==4) {
            DistanceCalculator d2 = new DistanceCalculator();
            d = d2.distance(node.x[dim], 0, x[dim], 0, "M");
            if(d<1)
                d=0;
            else if(d<5)
                d=.25;
            else if(d<10)
                d=.50;
            else if(d<50)
                d=.75;
            else
                d=1.00;

        }
        else  {
            DistanceCalculator d2 = new DistanceCalculator();
            d = d2.distance(0, node.x[dim], 0, x[dim], "M");
            if(d<1)
                d=0;
            else if(d<5)
                d=.25;
            else if(d<10)
                d=.50;
            else if(d<50)
                d=.75;
            else
                d=1.00;

        }

        // Log.e("SUBTREE ROOT",Double.toString(node.x[0]));
        if (d > d_min)
        {
            if (node.x[dim] > x[dim])
                check_subtree(node.Left, x);
            else
                check_subtree(node.Right, x);
        } else
        {
            check_subtree(node.Left, x);
            check_subtree(node.Right, x);
        }
    }

    public void set_bounding_cube(KDNode node, double[] x)
    {
        if (node == null)
            return;
        double d = 0;
        double dx;
        for (int k = 0; k < 6; k++)
        {
            dx = node.x[k] - x[k];

            if(k==0)
                dx= (node.x[k] - x[k])/5;
            else if(k==1)
                dx= (node.x[k] - x[k])/100;
            else if(k==2)
                dx= (node.x[k] - x[k])/100;
            else if(k==3)
                dx= (node.x[k] - x[k])/64;
            else if(k==4) {
                DistanceCalculator d2 = new DistanceCalculator();
                dx = d2.distance(node.x[4], 0, x[4], 0, "M");
                if(dx<1)
                    dx=0;
                else if(dx<5)
                    dx=.25;
                else if(dx<10)
                    dx=.50;
                else if(dx<50)
                    dx=.75;
                else
                    dx=1.00;

            }
            else if(k==5) {
                DistanceCalculator d2 = new DistanceCalculator();
                dx = d2.distance(0,node.x[5], 0, x[5], "M");
                if(dx<1)
                    dx=0;
                else if(dx<5)
                    dx=.25;
                else if(dx<10)
                    dx=.50;
                else if(dx<50)
                    dx=.75;
                else
                    dx=1.00;

            }
            if (dx > 0)
            {

                if (!max_boundary[k])
                {
                    if (dx > x_max[k])
                        x_max[k] = dx;
                    if (x_max[k] > d_min)
                    {
                        max_boundary[k] = true;
                        n_boundary++;
                    }
                }
            } else
            {

                if (!min_boundary[k])
                {
                    if (dx > x_min[k])
                        x_min[k] = dx;
                    if (x_min[k] > d_min)
                    {
                        min_boundary[k] = true;
                        n_boundary++;
                    }
                }
            }
            d = node.distance2(x,node.x,4);
            if (d > d_min)
                return;

        }

        if (d < d_min)
        {
            d_min = d;
            nearest_neighbour = node;
        }
    }

    public KDNode search_parent(KDNode parent, double[] x)
    {
        for (int k = 0; k < 6; k++)
        {
            x_min[k] = x_max[k] = 0;
            max_boundary[k] = min_boundary[k] = false; //
        }
        n_boundary = 0;

        KDNode search_root = parent;
        while (parent != null && (n_boundary != 6 * 6))
        {
            check_subtree(parent, x);
            search_root = parent;
            parent = parent.Parent;
        }

        return search_root;
    }
    //TODO END
    public void uncheck()
    {
        for (int n = 0; n < checked_nodes; n++)
            CheckedNodes[n].checked = false;
    }
    public KDNode FindParent(double[] x0)
    {
        KDNode parent = null;
        KDNode next = this.Root;
        int split;
        int key=0;
        while (key != 1 && next!=null)
        {
            split = next.axis;
            parent = next;
            System.out.println("(FIND PARENT CHECK " + parent.x[0] + " , " + parent.x[1] + ")");
            if(x0 == next.x){
                key=1;
            }
            else {
                if (x0[split] > next.x[split])
                    next = next.Right;
                else
                    next = next.Left;
            }
        }
        return parent;
    }
    public void Delete(double[] p)
    {
        //x = new double[2];
        KDNode parent = FindParent(p);
        System.out.println("(FIND PARENT CHECK FINAL " + parent.x[0] + " , " + parent.x[1] + ")");
        if(parent.Parent!=null)
            System.out.println("(FIND PARENT CHECK FINAL " + parent.Parent.x[0] + " , " + parent.Parent.x[1] + " , "+ parent.Parent.axis+")");

        if(parent.Parent!=null){

            if (parent.x[parent.Parent.axis] > parent.Parent.x[parent.Parent.axis])
            {
                if(parent.Right!=null){
                    parent.Parent.Right = parent.Right;
                    parent.Right.Parent = parent.Parent;
                    if(parent.Left!=null){
                        KDNode parent2 = FindParent(parent.Left.x);
                        parent.Left.Parent=parent2;
                        if (parent.Left.x[parent2.axis] > parent2.x[parent2.axis])
                        {
                            parent2.Right = parent.Left;
                            //newNode.orientation = true; //
                        } else
                        {
                            parent2.Left = parent.Left;
                            //newNode.orientation = false; //
                        }

                    }
                }
                else{
                    if(parent.Left!=null) {
                        parent.Parent.Right = parent.Left;
                        parent.Left.Parent = parent.Parent;
                    }

                    else{
                        parent.Parent.Right = null;
                    }

                }

            } else
            {
                if(parent.Right!=null){
                    parent.Parent.Left = parent.Right;
                    parent.Right.Parent = parent.Parent;
                    if(parent.Left!=null){
                        KDNode parent2 = FindParent(parent.Left.x);
                        parent.Left.Parent=parent2;
                        if (parent.Left.x[parent2.axis] > parent2.x[parent2.axis])
                        {
                            parent2.Right = parent.Left;
                            //newNode.orientation = true; //
                        } else
                        {
                            parent2.Left = parent.Left;
                            //newNode.orientation = false; //
                        }

                    }
                }
                else{
                    if(parent.Left!=null) {
                        parent.Parent.Left = parent.Left;
                        parent.Left.Parent = parent.Parent;
                    }


                    else{
                        parent.Parent.Left = null;
                    }

                }

            }

        }
        else{

            if(parent.Right!=null){
                this.Root = parent.Right;
                parent.Right.Parent = null;
                if(parent.Left!=null){
                    KDNode parent2 = FindParent(parent.Left.x);
                    parent.Left.Parent=parent2;
                    if (parent.Left.x[parent2.axis] > parent2.x[parent2.axis])
                    {
                        parent2.Right = parent.Left;
                        //newNode.orientation = true; //
                    } else
                    {
                        parent2.Left = parent.Left;
                        //newNode.orientation = false; //
                    }

                }
            }
            else{
                if(parent.Left!=null) {
                    this.Root = parent.Left;
                    parent.Left.Parent=null;
                }


                else{
                    this.Root = null;
                }

            }

        }

    }


}



/**
 * Created by mac on 20/04/17.
 */

public class KnnUsingKDTree {

    public KDTree kdt;

    public KnnUsingKDTree(int numpoints)
    {
        kdt = new KDTree(numpoints);
    }

    double knnnearestneighbour(KDTree kdt,double s[]){

        KDNode kdn = kdt.find_nearest(s);
        if (kdn!=null) {
            System.out.println("The nearest neighbor is: ");
            System.out.println("(" + kdn.x[0] + " , " + kdn.x[1] + " , " + kdn.x[2] + " , " + kdn.x[3] + " , " + kdn.x[4] + " , " + kdn.x[5] + ")");
            kdt.Delete(kdn.x);
            Log.i("ID SSS",Double.toString(kdn.x[6]));
            return kdn.x[6];
        }
        else
            return -1;


    }
}


