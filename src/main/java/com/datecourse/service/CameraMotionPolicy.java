package com.datecourse.service;

/** Prototype policy in screen units. No SDK calls, JavaFX timers or venue-specific exceptions. */
public final class CameraMotionPolicy {
    public enum Intent { FOCUS, RANGE_DRAG, RANGE_COMMIT, FIT, RESET }
    public enum Movement { NONE, PAN, COMBINED, IMMEDIATE }
    public record Frame(int level, int fitLevel, double screenDistance, double overflowRatio, boolean reducedMotion) {
        public Frame {
            if (level<1 || level>14 || fitLevel<1 || fitLevel>14 || !Double.isFinite(screenDistance)
                    || screenDistance<0 || !Double.isFinite(overflowRatio) || overflowRatio<0)
                throw new IllegalArgumentException("카메라 측정값을 확인해 주세요.");
        }
    }
    public record Options(double liveOverflowTolerance, double negligibleDistance, double nearDistance,
                          int minimumDuration, int maximumDuration) {
        public Options {
            if(liveOverflowTolerance<1 || negligibleDistance<0 || nearDistance<=negligibleDistance
                    || minimumDuration<0 || maximumDuration<minimumDuration)throw new IllegalArgumentException("이동 설정을 확인해 주세요.");
        }
        public static Options preview() { return new Options(1.04, .01, .85, 220, 600); }
    }
    public record Transition(Movement movement, int level, int duration) {}
    private final Options options;
    public CameraMotionPolicy() { this(Options.preview()); }
    public CameraMotionPolicy(Options options) { this.options=java.util.Objects.requireNonNull(options); }
    public Transition decide(Frame frame, Intent intent) {
        int level=frame.level();
        if(intent==Intent.RANGE_DRAG) {
            if(frame.overflowRatio()<=options.liveOverflowTolerance())return new Transition(Movement.NONE,level,0);
            level=Math.max(level,frame.fitLevel());
        } else if(intent==Intent.FIT || intent==Intent.RANGE_COMMIT || intent==Intent.RESET) level=frame.fitLevel();
        else level=Math.max(level,frame.fitLevel());
        if(intent==Intent.RESET)return new Transition(Movement.IMMEDIATE,level,0);
        if(frame.screenDistance()<=options.negligibleDistance() && level==frame.level())return new Transition(Movement.NONE,level,0);
        if(frame.reducedMotion())return new Transition(Movement.IMMEDIATE,level,0);
        int duration=(int)Math.round(Math.min(options.maximumDuration(),options.minimumDuration()
                +frame.screenDistance()*220+Math.abs(level-frame.level())*70));
        Movement move=level==frame.level() && frame.screenDistance()<=options.nearDistance()?Movement.PAN:Movement.COMBINED;
        return new Transition(move,level,duration);
    }
}
