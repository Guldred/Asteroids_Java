package game.component;


public class Vector2
{
    public static float TO_RADIANS = (1 / 180.0f) * (float) Math.PI;
    public static float TO_DEGREES = (1 / (float) Math.PI) * 180;
    public float x, y;

    public Vector2()
    {
    }

    public Vector2(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    public Vector2(Vector2 other)
    {
        this.x = other.x;
        this.y = other.y;
    }


    public Vector2 set(float x, float y)
    {
        this.x = x;
        this.y = y;


        return this;
    }


    public Vector2 set(Vector2 other)
    {

        this.x = other.x;
        this.y = other.y;

        return this;
    }


    public Vector2 add(float amount)
    {
        this.x += amount;
        this.y += amount;

        return this;
    }

    public Vector2 add(float x, float y)
    {
        this.x += x;
        this.y += y;

        return this;
    }


    public Vector2 add(Vector2 other)
    {
        this.x += other.x;
        this.y += other.y;

        return this;
    }


    public Vector2 sub (float amount)
    {
        this.x -= amount;
        this.y -= amount;

        return this;
    }

    public Vector2 sub(float x, float y)
    {
        this.x -= x;
        this.y -= y;

        return this;
    }


    public Vector2 sub(Vector2 other)
    {
        this.x -= other.x;
        this.y -= other.y;

        return this;
    }

    public Vector2 mul(float scalar)
    {
        this.x *= scalar;
        this.y *= scalar;

        return this;
    }

    public float magnitude()
    {
        return (float)Math.sqrt(x*x + y*y);
    }

    public Vector2 normalize()
    {
        float magnitude = magnitude();

        if(magnitude != 0)
        {
            this.x /= magnitude;
            this.y /= magnitude;
        }

        return this;
    }

    public float angle()
    {

        float angle = (float) Math.atan2(y, x) * TO_DEGREES;

        if(angle < 0)
        {
            angle += 360;
        }

        return angle;
    }

    /** Returns the distance between this vector and another vector. Note that the value returned is a scalar quantity. */
    public float distance(Vector2 other)
    {
        //Finds the difference in x and y components between this vector and the other vector. This finds the vector which points from the other vector to this vector.
        //It wouldn't make a difference if we reversed the subtraction, as the magnitude of the distance vector would remain the same.
        float distX = this.x - other.x;
        float distY = this.y - other.y;

        //Returns the magnitude of the distance between both vectors. This is done via the pythagorean theorem, using sqrt(x*x + y*y).
        return (float)Math.sqrt(distX*distX + distY*distY);
    }

    /** Returns the distance between this vector and the given x and y components, which form another vector. Note that the value returned is a scalar distance quantity. */
    public float distance(float x, float y)
    {
        //Finds the x and y distance between both vectors by subtracting their x and y components. Note which vector is subtracted by which other vector is irrelevent,
        //since the magnitude of the resulting vector will be the same.
        float distX = this.x - x;
        float distY = this.y - y;

        //Returns the magnitude of the distance between both vectors. This is done via the pythagorean theorem, using sqrt(x*x + y*y).
        return (float)Math.sqrt(distX*distX + distY*distY);
    }

    /** Returns the distance squared between this vector and another vector. Note that this avoids using a square root, making it more cost-efficient. Should be used
     *  instead of the Vector2.dist() method, as it prevents the costly square-root function. */
    public float distSquared(Vector2 other)
    {
        //Finds the x and y distance between both vectors by subtracting their x and y components. Note which vector is subtracted by which other vector is irrelevent,
        //since the magnitude of the resulting vector will be the same.
        float distX = this.x - other.x;
        float distY = this.y - other.y;

        //Returns the distance square between both vectors. This is done via the pythagorean theorem, using sqrt(x*x + y*y). However, we return the distance squared
        //by avoiding the square root. This is cost-efficient.
        return distX*distX + distY*distY;
    }

    /** Returns the distance squared between this vector and another vector with position (x,y). Note that this avoids using a square root, making it more cost-efficient.
     *  Should be used instead of the Vector2.dist() method, as it prevents the costly square-root function. */
    public float distSquared(float x, float y)
    {
        //Finds the x and y distance between both vectors by subtracting their x and y components. Note which vector is subtracted by which other vector is irrelevent,
        //since the magnitude of the resulting vector will be the same.
        float distX = this.x - x;
        float distY = this.y - y;

        //Returns the distance square between both vectors. This is done via the pythagorean theorem, using sqrt(x*x + y*y). However, we return the distance squared
        //by avoiding the square root. This is cost-efficient.
        return distX*distX + distY*distY;
    }

    public void addForce(float direction, float force) {
        this.x += Math.cos(Math.toRadians(direction)) * force;
        this.y += Math.sin(Math.toRadians(direction)) * force;
    }

    public String toString()
    {
        return "(" + x + ", " + y + ")";
    }
}









