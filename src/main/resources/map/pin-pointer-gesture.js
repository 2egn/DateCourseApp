(function () {
    'use strict';
    // Input state only: no SDK, DOM, clock, venue IDs, or preview rendering.
    window.PinPointerGesture = class PinPointerGesture {
        constructor(options) {
            if (!Number.isFinite(options.movementTolerance) || options.movementTolerance <= 0)
                throw new Error('A positive pointer movement tolerance is required');
            this.tolerance = options.movementTolerance;
            this.press = null;
            this.completed = null;
        }
        begin(point, pin) {
            this.completed = null;
            this.press = {x:point.x, y:point.y, pin:pin, dragged:false};
        }
        move(point) {
            if (this.press && this.distance(point, this.press) > this.tolerance) this.press.dragged = true;
        }
        get heldPin() { return this.press && !this.press.dragged ? this.press.pin : null; }
        finish(point, acceptsPin) {
            this.move(point);
            const press = this.press; this.press = null;
            if (!press || !press.pin && !press.dragged) return null;
            const cancelled = press.dragged || !press.pin || !acceptsPin(press.pin);
            this.completed = {pin:press.pin, x:point.x, y:point.y, cancelled:cancelled};
            return cancelled ? null : press.pin;
        }
        blocksSdkClick(pin) {
            return !!(this.press && this.press.pin) || !!this.completed && (this.completed.cancelled || this.completed.pin === pin);
        }
        consumesClick(point) { return !!this.completed && this.distance(point, this.completed) <= this.tolerance; }
        isDragAt(point) { return !!this.press && (this.press.dragged || this.distance(point, this.press) > this.tolerance); }
        cancelPress() { this.press = null; }
        reset() { this.press = null; this.completed = null; }
        distance(a, b) { return Math.hypot(a.x-b.x, a.y-b.y); }
    };
}());
