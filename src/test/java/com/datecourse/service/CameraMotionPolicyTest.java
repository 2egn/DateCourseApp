package com.datecourse.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CameraMotionPolicyTest {
    final CameraMotionPolicy policy=new CameraMotionPolicy();
    CameraMotionPolicy.Frame frame(int level,int fit,double distance,double overflow,boolean reduced){return new CameraMotionPolicy.Frame(level,fit,distance,overflow,reduced);}
    @Test void nearbySelectionKeepsZoomAndPans(){var action=policy.decide(frame(5,4,.3,.9,false),CameraMotionPolicy.Intent.FOCUS);assertEquals(CameraMotionPolicy.Movement.PAN,action.movement());assertEquals(5,action.level());}
    @Test void radiusDragDoesNotFightViewportWhileCircleFits(){assertEquals(CameraMotionPolicy.Movement.NONE,policy.decide(frame(5,3,.2,.95,false),CameraMotionPolicy.Intent.RANGE_DRAG).movement());}
    @Test void radiusDragNeverZoomsInAndReleaseCanFit(){assertEquals(6,policy.decide(frame(5,6,.1,1.6,false),CameraMotionPolicy.Intent.RANGE_DRAG).level());assertEquals(3,policy.decide(frame(5,3,.1,.3,false),CameraMotionPolicy.Intent.RANGE_COMMIT).level());}
    @Test void combinedMotionIsBoundedAndReducedMotionIsImmediate(){var action=policy.decide(frame(5,8,6,3,false),CameraMotionPolicy.Intent.FOCUS);assertEquals(CameraMotionPolicy.Movement.COMBINED,action.movement());assertTrue(action.duration()<=600);assertEquals(CameraMotionPolicy.Movement.IMMEDIATE,policy.decide(frame(5,8,6,3,true),CameraMotionPolicy.Intent.FOCUS).movement());}
    @Test void sameTargetIsNoOp(){assertEquals(CameraMotionPolicy.Movement.NONE,policy.decide(frame(5,5,0,1,false),CameraMotionPolicy.Intent.FOCUS).movement());}
}
