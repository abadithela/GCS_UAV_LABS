/* 
Copyright (C) 2001, 2006 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: Matrix.java 11951 2009-06-27 07:54:06Z patrickmurris $
 */
public class Matrix
{
    public static final Matrix IDENTITY = new Matrix(
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1,
        true);

    // Row 1
    public final double m11;
    public final double m12;
    public final double m13;
    public final double m14;
    // Row 2
    public final double m21;
    public final double m22;
    public final double m23;
    public final double m24;
    // Row 3
    public final double m31;
    public final double m32;
    public final double m33;
    public final double m34;
    // Row 4
    public final double m41;
    public final double m42;
    public final double m43;
    public final double m44;

    // 16 values in a 4x4 matrix.
    private static final int NUM_ELEMENTS = 16;
    // True when this matrix represents a 3D transform.
    private final boolean isOrthonormalTransform;
    // Cached computations.
    private int hashCode;

    public Matrix(double value)
    {
        // 'value' is placed in the diagonal.
        this(
            value, 0, 0, 0,
            0, value, 0, 0,
            0, 0, value, 0,
            0, 0, 0, value);
    }

    public Matrix(
        double m11, double m12, double m13, double m14,
        double m21, double m22, double m23, double m24,
        double m31, double m32, double m33, double m34,
        double m41, double m42, double m43, double m44)
    {
        this(
            m11, m12, m13, m14,
            m21, m22, m23, m24,
            m31, m32, m33, m34,
            m41, m42, m43, m44,
            false);
    }

    Matrix(
        double m11, double m12, double m13, double m14,
        double m21, double m22, double m23, double m24,
        double m31, double m32, double m33, double m34,
        double m41, double m42, double m43, double m44,
        boolean isOrthonormalTransform)
    {
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m14 = m14;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m24 = m24;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
        this.m34 = m34;
        this.m41 = m41;
        this.m42 = m42;
        this.m43 = m43;
        this.m44 = m44;
        this.isOrthonormalTransform = isOrthonormalTransform;
    }

    public final boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;

        Matrix that = (Matrix) obj;
        return (this.m11 == that.m11) && (this.m12 == that.m12) && (this.m13 == that.m13) && (this.m14 == that.m14)
            && (this.m21 == that.m21) && (this.m22 == that.m22) && (this.m23 == that.m23) && (this.m24 == that.m24)
            && (this.m31 == that.m31) && (this.m32 == that.m32) && (this.m33 == that.m33) && (this.m34 == that.m34)
            && (this.m41 == that.m41) && (this.m42 == that.m42) && (this.m43 == that.m43) && (this.m44 == that.m44);
    }

    public final int hashCode()
    {
        if (this.hashCode == 0)
        {
            int result;
            long tmp;
            tmp = Double.doubleToLongBits(this.m11);
            result = (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m12);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m13);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m14);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m21);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m22);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m23);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m24);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m31);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m32);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m33);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m34);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m41);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m42);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m43);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(this.m44);
            result = 29 * result + (int) (tmp ^ (tmp >>> 32));
            this.hashCode = result;
        }
        return this.hashCode;
    }

    public static Matrix fromArray(double[] compArray, int offset, boolean rowMajor)
    {
        if (compArray == null)
        {
            String msg = Logging.getMessage("nullValue.ArrayIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if ((compArray.length - offset) < NUM_ELEMENTS)
        {
            String msg = Logging.getMessage("generic.ArrayInvalidLength", compArray.length);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        
        if (rowMajor)
        {
            //noinspection PointlessArithmeticExpression
            return new Matrix(
                // Row 1
                compArray[0 + offset],
                compArray[1 + offset],
                compArray[2 + offset],
                compArray[3 + offset],
                // Row 2
                compArray[4 + offset],
                compArray[5 + offset],
                compArray[6 + offset],
                compArray[7 + offset],
                // Row 3
                compArray[8 + offset],
                compArray[9 + offset],
                compArray[10 + offset],
                compArray[11 + offset],
                // Row 4
                compArray[12 + offset],
                compArray[13 + offset],
                compArray[14 + offset],
                compArray[15 + offset]);
        }
        else
        {
            //noinspection PointlessArithmeticExpression
            return new Matrix(
                // Row 1
                compArray[0 + offset],
                compArray[4 + offset],
                compArray[8 + offset],
                compArray[12 + offset],
                // Row 2
                compArray[1 + offset],
                compArray[5 + offset],
                compArray[9 + offset],
                compArray[13 + offset],
                // Row 3
                compArray[2 + offset],
                compArray[6 + offset],
                compArray[10 + offset],
                compArray[14 + offset],
                // Row 4
                compArray[3 + offset],
                compArray[7 + offset],
                compArray[11 + offset],
                compArray[15 + offset]);
        }
    }

    public final double[] toArray(double[] compArray, int offset, boolean rowMajor)
    {
        if (compArray == null)
        {
            String msg = Logging.getMessage("nullValue.ArrayIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if ((compArray.length - offset) < NUM_ELEMENTS)
        {
            String msg = Logging.getMessage("generic.ArrayInvalidLength", compArray.length);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (rowMajor)
        {
            // Row 1
            //noinspection PointlessArithmeticExpression
            compArray[0 + offset] = this.m11;
            compArray[1 + offset] = this.m12;
            compArray[2 + offset] = this.m13;
            compArray[3 + offset] = this.m14;
            // Row 2
            compArray[4 + offset] = this.m21;
            compArray[5 + offset] = this.m22;
            compArray[6 + offset] = this.m23;
            compArray[7 + offset] = this.m24;
            // Row 3
            compArray[8 + offset] = this.m31;
            compArray[9 + offset] = this.m32;
            compArray[10 + offset] = this.m33;
            compArray[11 + offset] = this.m34;
            // Row 4
            compArray[12 + offset] = this.m41;
            compArray[13 + offset] = this.m42;
            compArray[14 + offset] = this.m43;
            compArray[15 + offset] = this.m44;
        }
        else
        {
            // Row 1
            //noinspection PointlessArithmeticExpression
            compArray[0 + offset] = this.m11;
            compArray[4 + offset] = this.m12;
            compArray[8 + offset] = this.m13;
            compArray[12 + offset] = this.m14;
            // Row 2
            compArray[1 + offset] = this.m21;
            compArray[5 + offset] = this.m22;
            compArray[9 + offset] = this.m23;
            compArray[13 + offset] = this.m24;
            // Row 3
            compArray[2 + offset] = this.m31;
            compArray[6 + offset] = this.m32;
            compArray[10 + offset] = this.m33;
            compArray[14 + offset] = this.m34;
            // Row 4
            compArray[3 + offset] = this.m41;
            compArray[7 + offset] = this.m42;
            compArray[11 + offset] = this.m43;
            compArray[15 + offset] = this.m44;
        }

        return compArray;
    }

    public final String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(this.m11).append(", ").append(this.m12).append(", ").append(this.m13).append(", ").append(this.m14);
        sb.append(", \r\n");
        sb.append(this.m21).append(", ").append(this.m22).append(", ").append(this.m23).append(", ").append(this.m24);
        sb.append(", \r\n");
        sb.append(this.m31).append(", ").append(this.m32).append(", ").append(this.m33).append(", ").append(this.m34);
        sb.append(", \r\n");
        sb.append(this.m41).append(", ").append(this.m42).append(", ").append(this.m43).append(", ").append(this.m44);
        sb.append(")");
        return sb.toString();
    }

    public final double getM11()
    {
        return this.m11;
    }

    public final double getM12()
    {
        return this.m12;
    }

    public final double getM13()
    {
        return this.m13;
    }

    public final double getM14()
    {
        return this.m14;
    }

    public final double getM21()
    {
        return this.m21;
    }

    public final double getM22()
    {
        return this.m22;
    }

    public final double getM23()
    {
        return this.m23;
    }

    public final double getM24()
    {
        return this.m24;
    }

    public final double getM31()
    {
        return this.m31;
    }

    public final double getM32()
    {
        return this.m32;
    }

    public final double getM33()
    {
        return this.m33;
    }

    public final double getM34()
    {
        return this.m34;
    }

    public final double getM41()
    {
        return this.m41;
    }

    public final double getM42()
    {
        return this.m42;
    }

    public final double getM43()
    {
        return this.m43;
    }

    public final double getM44()
    {
        return this.m44;
    }

    public final double m11()
    {
        return this.m11;
    }

    public final double m12()
    {
        return this.m12;
    }

    public final double m13()
    {
        return this.m13;
    }

    public final double m14()
    {
        return this.m14;
    }

    public final double m21()
    {
        return this.m21;
    }

    public final double m22()
    {
        return this.m22;
    }

    public final double m23()
    {
        return this.m23;
    }

    public final double m24()
    {
        return this.m24;
    }

    public final double m31()
    {
        return this.m31;
    }

    public final double m32()
    {
        return this.m32;
    }

    public final double m33()
    {
        return this.m33;
    }

    public final double m34()
    {
        return this.m34;
    }

    public final double m41()
    {
        return this.m41;
    }

    public final double m42()
    {
        return this.m42;
    }

    public final double m43()
    {
        return this.m43;
    }

    public final double m44()
    {
        return this.m44;
    }

    // ============== Factory Functions ======================= //
    // ============== Factory Functions ======================= //
    // ============== Factory Functions ======================= //

    public static Matrix fromAxisAngle(Angle angle, Vec4 axis)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (axis == null)
        {
            String msg = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return fromAxisAngle(angle, axis.x, axis.y, axis.z, true);
    }

    public static Matrix fromAxisAngle(Angle angle, double axisX, double axisY, double axisZ)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        return fromAxisAngle(angle, axisX, axisY, axisZ, true);
    }

    private static Matrix fromAxisAngle(Angle angle, double axisX, double axisY, double axisZ, boolean normalize)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (normalize)
        {
            double length = Math.sqrt((axisX * axisX) + (axisY * axisY) + (axisZ * axisZ));
            if (!isZero(length) && (length != 1.0))
            {
                axisX /= length;
                axisY /= length;
                axisZ /= length;
            }
        }

        double c = angle.cos();
        double s = angle.sin();
        double one_minus_c = 1.0 - c;
        return new Matrix(
            // Row 1
            c + (one_minus_c * axisX * axisX),
            (one_minus_c * axisX * axisY) - (s * axisZ),
            (one_minus_c * axisX * axisZ) + (s * axisY),
            0.0,
            // Row 2
            (one_minus_c * axisX * axisY) + (s * axisZ),
            c + (one_minus_c * axisY * axisY),
            (one_minus_c * axisY * axisZ) - (s * axisX),
            0.0,
            // Row 3
            (one_minus_c * axisX * axisZ) - (s * axisY),
            (one_minus_c * axisY * axisZ) + (s * axisX),
            c + (one_minus_c * axisZ * axisZ),
            0.0,
            // Row 4
            0.0, 0.0, 0.0, 1.0,
            // Rotation matrices are orthogonal, 3D transforms.
            true);
    }

    public static Matrix fromQuaternion(Quaternion quaternion)
    {
        if (quaternion == null)
        {
            String msg = Logging.getMessage("nullValue.QuaternionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return fromQuaternion(quaternion.x, quaternion.y, quaternion.z, quaternion.w, true);
    }

    private static Matrix fromQuaternion(double x, double y, double z, double w, boolean normalize)
    {
        if (normalize)
        {
            double length = Math.sqrt((x * x) + (y * y) + (z * z)+ (w * w));
            if (!isZero(length) && (length != 1.0))
            {
                x /= length;
                y /= length;
                z /= length;
                w /= length;
            }
        }

        return new Matrix(
            // Row 1
            1.0 - (2.0 * y * y) - (2.0 * z * z),
            (2.0 * x * y) - (2.0 * z * w),
            (2.0 * x * z) + (2.0 * y * w),
            0.0,
            // Row 2
            (2.0 * x * y) + (2.0 * z * w),
            1.0 - (2.0 * x * x) - (2.0 * z * z),
            (2.0 * y * z) - (2.0 * x * w),
            0.0,
            // Row 3
            (2.0 * x * z) - (2.0 * y * w),
            (2.0 * y * z) + (2.0 * x * w),
            1.0 - (2.0 * x * x) - (2.0 * y * y),
            0.0,
            // Row 4
            0.0, 0.0, 0.0, 1.0,
            // Rotation matrices are orthogonal, 3D transforms.
            true);
    }

    public static Matrix fromRotationXYZ(Angle xRotation, Angle yRotation, Angle zRotation)
    {
        if ((xRotation == null) || (yRotation == null) || (zRotation == null))
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double cx = xRotation.cos();
        double cy = yRotation.cos();
        double cz = zRotation.cos();
        double sx = xRotation.sin();
        double sy = yRotation.sin();
        double sz = zRotation.sin();
        return new Matrix(
            cy * cz, -cy * sz, sy, 0.0,
            (sx * sy * cz) + (cx * sz), -(sx * sy * sz) + (cx * cz), -sx * cy, 0.0,
            -(cx * sy * cz) + (sx * sz), (cx * sy * sz) + (sx * cz), cx * cy, 0.0,
            0.0, 0.0, 0.0, 1.0,
            // Rotation matrices are orthogonal, 3D transforms.
            true);
    }

    public static Matrix fromRotationX(Angle angle)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double c = angle.cos();
        double s = angle.sin();
        return new Matrix(
            1.0, 0.0, 0.0, 0.0,
            0.0, c, -s, 0.0,
            0.0, s, c, 0.0,
            0.0, 0.0, 0.0, 1.0,
            // Rotation matrices are orthogonal, 3D transforms.
            true);
    }

    public static Matrix fromRotationY(Angle angle)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double c = angle.cos();
        double s = angle.sin();
        return new Matrix(
            c, 0.0, s, 0.0,
            0.0, 1.0, 0.0, 0.0,
            -s, 0.0, c, 0.0,
            0.0, 0.0, 0.0, 1.0,
            // Rotation matrices are orthogonal, 3D transforms.
            true);
    }

    public static Matrix fromRotationZ(Angle angle)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double c = angle.cos();
        double s = angle.sin();
        return new Matrix(
            c, -s, 0.0, 0.0,
            s, c, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0,
            // Rotation matrices are orthogonal, 3D transforms.
            true);
    }

    public static Matrix fromScale(double scale)
    {
        return fromScale(scale, scale, scale);
    }

    public static Matrix fromScale(Vec4 scale)
    {
        if (scale == null)
        {
            String msg = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return fromScale(scale.x, scale.y, scale.z);
    }

    public static Matrix fromScale(double scaleX, double scaleY, double scaleZ)
    {
        return new Matrix(
            scaleX, 0.0, 0.0, 0.0,
            0.0, scaleY, 0.0, 0.0,
            0.0, 0.0, scaleZ, 0.0,
            0.0, 0.0, 0.0, 1.0,
            // Scale matrices are non-orthogonal, 3D transforms.
            false);
    }

    public static Matrix fromTranslation(Vec4 translation)
    {
        if (translation == null)
        {
            String msg = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return fromTranslation(translation.x, translation.y, translation.z);
    }

    public static Matrix fromTranslation(double x, double y, double z)
    {
        return new Matrix(
            1.0, 0.0, 0.0, x,
            0.0, 1.0, 0.0, y,
            0.0, 0.0, 1.0, z,
            0.0, 0.0, 0.0, 1.0,
            // Translation matrices are orthogonal, 3D transforms.
            true);
    }

    public static Matrix fromViewLookAt(Vec4 eye, Vec4 center, Vec4 up)
    {
        if (eye == null || center == null || up == null)
        {
            String msg = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 forward = center.subtract3(eye);
        Vec4 f = forward.normalize3();

        Vec4 s = f.cross3(up);
        s = s.normalize3();

        Vec4 u = s.cross3(f);
        u = u.normalize3();

        Matrix mAxes = new Matrix(
            s.x,  s.y,  s.z, 0.0,
            u.x,  u.y,  u.z, 0.0,
           -f.x, -f.y, -f.z, 0.0,
            0.0,  0.0,  0.0, 1.0,
            true);
        Matrix mEye = Matrix.fromTranslation(
            -eye.x, -eye.y, -eye.z);
        return mAxes.multiply(mEye);
    }

    public static Matrix fromModelLookAt(Vec4 eye, Vec4 center, Vec4 up)
    {
        if (eye == null || center == null || up == null)
        {
            String msg = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 forward = center.subtract3(eye);
        Vec4 f = forward.normalize3();

        Vec4 s = up.cross3(f);
        s = s.normalize3();

        Vec4 u = f.cross3(s);
        u = u.normalize3();

        Matrix mAxes = new Matrix(
                s.x, u.x, f.x, 0.0,
                s.y, u.y, f.y, 0.0,
                s.z, u.z, f.z, 0.0,
                0.0, 0.0, 0.0, 1.0,
                true);
        Matrix mEye = Matrix.fromTranslation(
                eye.x, eye.y, eye.z);
        return mEye.multiply(mAxes);
    }

    public static Matrix fromPerspective(Angle horizontalFieldOfView, double viewportWidth, double viewportHeight,
        double near, double far)
    {
        if (horizontalFieldOfView == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double fovX = horizontalFieldOfView.degrees;
        if (fovX <= 0.0 || fovX > 180.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", "horizontalFieldOfView=" + fovX);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (viewportWidth <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", "viewportWidth=" + viewportWidth);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (viewportHeight <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", "viewportHeight=" + viewportHeight);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (near <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", "near=" + near);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (far <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", "far=" + far);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (far <= near)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", "far=" + far + ",near=" + near);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double f = 1.0 / horizontalFieldOfView.tanHalfAngle();
        // We are using *horizontal* field-of-view here. This results in a different matrix than documented in sources
        // using vertical field-of-view.
        return new Matrix(
            f, 0.0, 0.0, 0.0,
            0.0, (f * viewportWidth) / viewportHeight, 0.0, 0.0,
            0.0, 0.0, -(far + near) / (far - near), -(2.0 * far * near) / (far - near),
            0.0, 0.0, -1.0, 0.0);
    }

    public static Matrix fromPerspective(double width, double height, double near, double far)
    {
        if (width <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", width);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (height <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", height);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (near <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", near);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (far <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", far);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (far <= near)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", far);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return new Matrix(
            2.0 / width, 0.0, 0.0, 0.0,
            0.0, (2.0 * near) / height, 0.0, 0.0,
            0.0, 0.0, -(far + near) / (far - near), -(2.0 * far * near) / (far - near),
            0.0, 0.0, -1.0, 0.0);
    }

    public static Matrix fromOrthographic(double width, double height, double near, double far)
    {
        if (width <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", width);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (height <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", height);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (near <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", near);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (far <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", far);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (far <= near)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", far);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return new Matrix(
            2.0 / width, 0.0, 0.0, 0.0,
            0.0, 2.0 / height, 0.0, 0.0,
            0.0, 0.0, -2.0 / (far - near), -(far + near) / (far - near),
            0.0, 0.0, 0.0, 1.0);
    }

    public static Matrix fromOrthographic2D(double width, double height)
    {
        if (width <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", width);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (height <= 0.0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", height);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return new Matrix(
            2.0 / width, 0.0, 0.0, 0.0,
            0.0, 2.0 / height, 0.0, 0.0,
            0.0, 0.0, -1.0, 0.0,
            0.0, 0.0, 0.0, 1.0);
    }

    /**
     * Computes a <code>Matrix</code> that will map a aligned 2D grid coordinates to geographic coordinates in degrees.
     * It is assumed that the destination grid is parallel with lines of latitude and longitude, and has its origin
     * in the upper left hand corner.
     *
     * @param sector the grid sector.
     * @param imageWidth the grid width.
     * @param imageHeight the grid height.
     *
     * @return <code>Matrix</code> that will map from grid coordinates to geographic coordinates in degrees.
     * @throws IllegalArgumentException if <code>sector</code> is null, or if either <code>width</code> or
     *                                  <code>height</code> are less than 1.
     */
    public static Matrix fromImageToGeographic(int imageWidth, int imageHeight, Sector sector)
    {
        if (imageWidth < 1 || imageHeight < 1)
        {
            String message = Logging.getMessage("generic.InvalidImageSize", imageWidth, imageHeight);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Transform from grid coordinates to geographic coordinates. Since the grid is parallel with lines of latitude
        // and longitude, this is a simple scale and translation.

        double sx = sector.getDeltaLonDegrees() / imageWidth;
        double sy = -sector.getDeltaLatDegrees() / imageHeight;
        double tx = sector.getMinLongitude().degrees;
        double ty = sector.getMaxLatitude().degrees;

        return new Matrix(
            sx,  0.0, tx,  0.0,
            0.0, sy,  ty,  0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 0.0);
    }

    public static Matrix fromImageToGeographic(AVList worldFileParams)
    {
        if (worldFileParams == null)
        {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Transform from geographic coordinates to source grid coordinates. Start with the following system of
        // equations. The values a-f are defined by the world file, which construct and affine transform mapping grid
        // coordinates to geographic coordinates. We can simply plug these into the upper 3x3 values of our matrix.
        //
        // | a b c |   | x |   | lon |
        // | d e f | * | y | = | lat |
        // | 0 0 1 |   | 1 |   | 1   |

        Double a = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_X_PIXEL_SIZE);
        Double d = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_Y_COEFFICIENT);
        Double b = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_X_COEFFICIENT);
        Double e = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_Y_PIXEL_SIZE);
        Double c = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_X_LOCATION);
        Double f = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_Y_LOCATION);

        if (a == null || b == null || c == null || d == null || e == null || f == null)
        {
            return null;
        }

        return new Matrix(
            a,   b,   c,   0.0,
            d,   e,   f,   0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 0.0);
    }

    public static Matrix fromGeographicToImage(AVList worldFileParams)
    {
        if (worldFileParams == null)
        {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Transform from geographic coordinates to source grid coordinates. Start with the following system of
        // equations. The values a-f are defined by the world file, which construct and affine transform mapping grid
        // coordinates to geographic coordinates. We want to find the transform that maps geographic coordinates to
        // grid coordinates.
        //
        // | a b c |   | x |   | lon |
        // | d e f | * | y | = | lat |
        // | 0 0 1 |   | 1 |   | 1   |
        //
        // Expanding the matrix multiplication:
        //
        // a*x + b*y + c = lon
        // d*x + e*y + f = lat
        //
        // Then solving for x and y by eliminating variables:
        //
        // x0 = d - (e*a)/b
        // y0 = e - (d*b)/a
        // (-e/(b*x0))*lon + (1/x0)*lat + (e*c)/(b*x0) - f/x0 = x
        // (-d/(a*y0))*lon + (1/y0)*lat + (d*c)/(a*y0) - f/y0 = y
        //
        // And extracting new the matrix coefficients a'-f':
        //
        // a' = -e/(b*x0)
        // b' = 1/x0
        // c' = (e*c)/(b*x0) - f/x0
        // d' = -d/(a*y0)
        // e' = 1/y0
        // f' = (d*c)/(a*y0) - f/y0
        //
        // If b==0 and d==0, then we have the equation simplifies to:
        //
        // (1/a)*lon + (-c/a) = x
        // (1/e)*lat + (-f/e) = y
        //
        // And and the new matrix coefficients will be:
        //
        // a' = 1/a
        // b' = 0
        // c' = -c/a
        // d' = 0
        // e' = 1/e
        // f' = -f/e

        Double a = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_X_PIXEL_SIZE);
        Double d = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_Y_COEFFICIENT);
        Double b = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_X_COEFFICIENT);
        Double e = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_Y_PIXEL_SIZE);
        Double c = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_X_LOCATION);
        Double f = AVListImpl.getDoubleValue(worldFileParams, WorldFile.WORLD_FILE_Y_LOCATION);

        if (a == null || b == null || c == null || d == null || e == null || f == null)
        {
            return null;
        }

        if (b == 0.0 && d == 0.0)
        {
            return new Matrix(
                1.0/a, 0.0,   (-c/a), 0.0,
                0.0,   1.0/e, (-f/e), 0.0,
                0.0,   0.0,   1.0,    0.0,
                0.0,   0.0,   0.0,    0.0);
        }
        else
        {
            double x0 = d - (e*a)/b;
            double ap = -e/(b*x0);
            double bp = 1.0/x0;
            double cp = (e*c)/(b*x0) - f/x0;

            double y0 = e - (d*b)/a;
            double dp = -d/(a*y0);
            double ep = 1.0/y0;
            double fp = (d*c)/(a*y0) - f/y0;

            return new Matrix(
                ap,  bp,  cp,  0.0,
                dp,  ep,  fp,  0.0,
                0.0, 0.0, 1.0, 0.0,
                0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Computes a <code>Matrix</code> that will map constrained 2D grid coordinates to geographic coordinates in
     * degrees. The grid is defined by three control points. Each control point maps a location in the source grid to a
     * geographic location.
     *
     * @param imagePoints three control points in the source grid.
     * @param geoPoints three geographic locations corresponding to each grid control point.
     *
     * @return <code>Matrix</code> that will map from geographic coordinates to grid coordinates in degrees.
     * @throws IllegalArgumentException if either <code>imagePoints</code> or <code>geoPoints</code> is null or
     *                                  have length less than 3.
     */
    public static Matrix fromImageToGeographic(java.awt.geom.Point2D[] imagePoints, LatLon[] geoPoints)
    {
        if (imagePoints == null)
        {
            String message = Logging.getMessage("nullValue.ImagePointsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (geoPoints == null)
        {
            String message = Logging.getMessage("nullValue.GeoPointsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (imagePoints.length < 3)
        {
            String message = Logging.getMessage("generic.ArrayInvalidLength", "imagePoints.length < 3");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (geoPoints.length < 3)
        {
            String message = Logging.getMessage("generic.ArrayInvalidLength", "geoPoints.length < 3");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Transform from geographic coordinates to source grid coordinates. Start with the following system of
        // equations. The values a-f are the unknown coefficients we want to derive, The (lat,lon) and (x,y)
        // coordinates are constants defined by the caller via geoPoints and imagePoints, respectively.
        //
        // | a b c |   | x1 x2 x3 |   | lon1 lon2 lon3 |
        // | d e f | * | y1 y2 y3 | = | lat1 lat2 lat3 |
        // | 0 0 1 |   | 1  1  1  |   | 1    1    1    |
        //
        // Expanding the matrix multiplication:
        //
        // a*x1 + b*y1 + c = lon1
        // a*x2 + b*y2 + c = lon2
        // a*x3 + b*y3 + c = lon3
        // d*x1 + e*y1 + f = lat1
        // d*x2 + e*y2 + f = lat2
        // d*x3 + e*y3 + f = lat3
        //
        // Then solving for a-c, and d-f by repeatedly eliminating variables:
        //
        // a0 = (x3-x1) - (x2-x1)*(y3-y1)/(y2-y1)
        // a = (1/a0) * [(lon3-lon1) - (lon2-lon1)*(y3-y1)/(y2-y1)]
        // b = (lon2-lon1)/(y2-y1) - a*(x2-x1)/(y2-y1)
        // c = lon1 - a*x1 - b*y1
        //
        // d0 = (x3-x1) - (x2-x1)*(y3-y1)/(y2-y1)
        // d = (1/d0) * [(lat3-lat1) - (lat2-lat1)*(y3-y1)/(y2-y1)]
        // e = (lat2-lat1)/(y2-y1) - d*(x2-x1)/(y2-y1)
        // f = lat1 - d*x1 - e*y1

        double lat1 = geoPoints[0].getLatitude().degrees;
        double lat2 = geoPoints[1].getLatitude().degrees;
        double lat3 = geoPoints[2].getLatitude().degrees;
        double lon1 = geoPoints[0].getLongitude().degrees;
        double lon2 = geoPoints[1].getLongitude().degrees;
        double lon3 = geoPoints[2].getLongitude().degrees;

        double x1 = imagePoints[0].getX();
        double x2 = imagePoints[1].getX();
        double x3 = imagePoints[2].getX();
        double y1 = imagePoints[0].getY();
        double y2 = imagePoints[1].getY();
        double y3 = imagePoints[2].getY();

        double a0 = (x3-x1) - (x2-x1)*(y3-y1)/(y2-y1);
        double a = (1/a0) * ((lon3-lon1) - (lon2-lon1)*(y3-y1)/(y2-y1));
        double b = (lon2-lon1)/(y2-y1) - a*(x2-x1)/(y2-y1);
        double c = lon1 - a*x1 - b*y1;

        double d0 = (x3-x1) - (x2-x1)*(y3-y1)/(y2-y1);
        double d = (1/d0) * ((lat3-lat1) - (lat2-lat1)*(y3-y1)/(y2-y1));
        double e = (lat2-lat1)/(y2-y1) - d*(x2-x1)/(y2-y1);
        double f = lat1 - d*x1 - e*y1;

        return new Matrix(
            a,   b,   c,   0.0,
            d,   e,   f,   0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 0.0);
    }

    public static Matrix fromGeographicToImage(java.awt.geom.Point2D[] imagePoints, LatLon[] geoPoints)
    {
        if (imagePoints == null)
        {
            String message = Logging.getMessage("nullValue.ImagePointsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (geoPoints == null)
        {
            String message = Logging.getMessage("nullValue.GeoPointsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (imagePoints.length < 3)
        {
            String message = Logging.getMessage("generic.ArrayInvalidLength", "imagePoints.length < 3");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (geoPoints.length < 3)
        {
            String message = Logging.getMessage("generic.ArrayInvalidLength", "geoPoints.length < 3");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Transform from geographic coordinates to source grid coordinates. Start with the following system of
        // equations. The values a-f are the unknown coefficients we want to derive, The (lat,lon) and (x,y)
        // coordinates are constants defined by the caller via geoPoints and imagePoints, respectively.
        //
        // | a b c |   | lon1 lon2 lon3 |   | x1 x2 x3 |
        // | d e f | * | lat1 lat2 lat3 | = | y1 y2 y3 |
        // | 0 0 1 |   | 1    1    1    |   | 1  1  1  |
        //
        // Expanding the matrix multiplication:
        //
        // a*lon1 + b*lat1 + c = x1
        // a*lon2 + b*lat2 + c = x2
        // a*lon3 + b*lat3 + c = x3
        // d*lon1 + e*lat1 + f = y1
        // d*lon2 + e*lat2 + f = y2
        // d*lon3 + e*lat3 + f = y3
        //
        // Then solving for a-c, and d-f by repeatedly eliminating variables:
        //
        // a0 = (lon3-lon1) - (lon2-lon1)*(lat3-lat1)/(lat2-lat1)
        // a = (1/a0) * [(x3-x1) - (x2-x1)*(lat3-lat1)/(lat2-lat1)]
        // b = (x2-x1)/(lat2-lat1) - a*(lon2-lon1)/(lat2-lat1)
        // c = x1 - a*lon1 - b*lat1
        //
        // d0 = (lon3-lon1) - (lon2-lon1)*(lat3-lat1)/(lat2-lat1)
        // d = (1/d0) * [(y3-y1) - (y2-y1)*(lat3-lat1)/(lat2-lat1)]
        // e = (y2-y1)/(lat2-lat1) - d*(lon2-lon1)/(lat2-lat1)
        // f = y1 - d*lon1 - e*lat1

        double lat1 = geoPoints[0].getLatitude().degrees;
        double lat2 = geoPoints[1].getLatitude().degrees;
        double lat3 = geoPoints[2].getLatitude().degrees;
        double lon1 = geoPoints[0].getLongitude().degrees;
        double lon2 = geoPoints[1].getLongitude().degrees;
        double lon3 = geoPoints[2].getLongitude().degrees;

        double x1 = imagePoints[0].getX();
        double x2 = imagePoints[1].getX();
        double x3 = imagePoints[2].getX();
        double y1 = imagePoints[0].getY();
        double y2 = imagePoints[1].getY();
        double y3 = imagePoints[2].getY();

        double a0 = (lon3-lon1) - (lon2-lon1)*(lat3-lat1)/(lat2-lat1);
        double a = (1/a0) * ((x3-x1) - (x2-x1)*(lat3-lat1)/(lat2-lat1));
        double b = (x2-x1)/(lat2-lat1) - a*(lon2-lon1)/(lat2-lat1);
        double c = x1 - a*lon1 - b*lat1;

        double d0 = (lon3-lon1) - (lon2-lon1)*(lat3-lat1)/(lat2-lat1);
        double d = (1/d0) * ((y3-y1) - (y2-y1)*(lat3-lat1)/(lat2-lat1));
        double e = (y2-y1)/(lat2-lat1) - d*(lon2-lon1)/(lat2-lat1);
        double f = y1 - d*lon1 - e*lat1;

        return new Matrix(
            a,   b,   c,   0.0,
            d,   e,   f,   0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Computes a Matrix that will map the geographic region defined by sector onto a Cartesian region of the specified
     * <code>width</code> and <code>height</code> and centered at the point <code>(x, y)</code>.
     *
     * @param sector the geographic region which will be mapped to the Cartesian region
     * @param x x-coordinate of lower left hand corner of the Cartesian region
     * @param y y-coordinate of lower left hand corner of the Cartesian region
     * @param width width of the Cartesian region, extending to the right from the x-coordinate
     * @param height height of the Cartesian region, extending up from the y-coordinate
     *
     * @return Matrix that will map from the geographic region to the Cartesian region.
     * @throws IllegalArgumentException if <code>sector</code> is null, or if <code>width</code> or <code>height</code>
     *                                  are less than zero.
     */
    public static Matrix fromGeographicToViewport(Sector sector, int x, int y, int width, int height)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (width <= 0)
        {
            String message = Logging.getMessage("Geom.WidthInvalid", width);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (height <= 0)
        {
            String message = Logging.getMessage("Geom.HeightInvalid", height);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Matrix transform = Matrix.IDENTITY;
        transform = transform.multiply(
            Matrix.fromTranslation(-x, -y, 0.0));
        transform = transform.multiply(
            Matrix.fromScale(width / sector.getDeltaLonDegrees(), height / sector.getDeltaLatDegrees(), 1.0));
        transform = transform.multiply(
            Matrix.fromTranslation(-sector.getMinLongitude().degrees, -sector.getMinLatitude().degrees, 0.0));

        return transform;
    }


    /**
     * Computes a Matrix that will map a Cartesian region of the specified <code>width</code> and <code>height</code>
     * and centered at the point <code>(x, y)</code> to the geographic region defined by sector onto .
     *
     * @param sector the geographic region the Cartesian region will be mapped to
     * @param x x-coordinate of lower left hand corner of the Cartesian region
     * @param y y-coordinate of lower left hand corner of the Cartesian region
     * @param width width of the Cartesian region, extending to the right from the x-coordinate
     * @param height height of the Cartesian region, extending up from the y-coordinate
     *
     * @return Matrix that will map from Cartesian region to the geographic region.
     * @throws IllegalArgumentException if <code>sector</code> is null, or if <code>width</code> or <code>height</code>
     *                                  are less than zero.
     */
    public static Matrix fromViewportToGeographic(Sector sector, int x, int y, int width, int height)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (width <= 0)
        {
            String message = Logging.getMessage("Geom.WidthInvalid", width);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (height <= 0)
        {
            String message = Logging.getMessage("Geom.HeightInvalid", height);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Matrix transform = Matrix.IDENTITY;
        transform = transform.multiply(
            Matrix.fromTranslation(sector.getMinLongitude().degrees, sector.getMinLatitude().degrees, 0.0));
        transform = transform.multiply(
            Matrix.fromScale(sector.getDeltaLonDegrees() / width, sector.getDeltaLatDegrees() / height, 1.0));
        transform = transform.multiply(
            Matrix.fromTranslation(x, y, 0.0));

        return transform;
    }

    // ============== Arithmetic Functions ======================= //
    // ============== Arithmetic Functions ======================= //
    // ============== Arithmetic Functions ======================= //

    public final Matrix add(Matrix matrix)
    {
        if (matrix == null)
        {
            String msg = Logging.getMessage("nullValue.MatrixIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return new Matrix(
            this.m11 + matrix.m11, this.m12 + matrix.m12, this.m13 + matrix.m13, this.m14 + matrix.m14,
            this.m21 + matrix.m21, this.m22 + matrix.m22, this.m23 + matrix.m23, this.m24 + matrix.m24,
            this.m31 + matrix.m31, this.m32 + matrix.m32, this.m33 + matrix.m33, this.m34 + matrix.m34,
            this.m41 + matrix.m41, this.m42 + matrix.m42, this.m43 + matrix.m43, this.m44 + matrix.m44);
    }

    public final Matrix subtract(Matrix matrix)
    {
        if (matrix == null)
        {
            String msg = Logging.getMessage("nullValue.MatrixIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return new Matrix(
            this.m11 - matrix.m11, this.m12 - matrix.m12, this.m13 - matrix.m13, this.m14 - matrix.m14,
            this.m21 - matrix.m21, this.m22 - matrix.m22, this.m23 - matrix.m23, this.m24 - matrix.m24,
            this.m31 - matrix.m31, this.m32 - matrix.m32, this.m33 - matrix.m33, this.m34 - matrix.m34,
            this.m41 - matrix.m41, this.m42 - matrix.m42, this.m43 - matrix.m43, this.m44 - matrix.m44);
    }

    public final Matrix multiplyComponents(double value)
    {
        return new Matrix(
            this.m11 * value, this.m12 * value, this.m13 * value, this.m14 * value,
            this.m21 * value, this.m22 * value, this.m23 * value, this.m24 * value,
            this.m31 * value, this.m32 * value, this.m33 * value, this.m34 * value,
            this.m41 * value, this.m42 * value, this.m43 * value, this.m44 * value);
    }

    public final Matrix multiply(Matrix matrix)
    {
        if (matrix == null)
        {
            String msg = Logging.getMessage("nullValue.MatrixIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return new Matrix(
            // Row 1
            (this.m11 * matrix.m11) + (this.m12 * matrix.m21) + (this.m13 * matrix.m31) + (this.m14 * matrix.m41),
            (this.m11 * matrix.m12) + (this.m12 * matrix.m22) + (this.m13 * matrix.m32) + (this.m14 * matrix.m42),
            (this.m11 * matrix.m13) + (this.m12 * matrix.m23) + (this.m13 * matrix.m33) + (this.m14 * matrix.m43),
            (this.m11 * matrix.m14) + (this.m12 * matrix.m24) + (this.m13 * matrix.m34) + (this.m14 * matrix.m44),
            // Row 2
            (this.m21 * matrix.m11) + (this.m22 * matrix.m21) + (this.m23 * matrix.m31) + (this.m24 * matrix.m41),
            (this.m21 * matrix.m12) + (this.m22 * matrix.m22) + (this.m23 * matrix.m32) + (this.m24 * matrix.m42),
            (this.m21 * matrix.m13) + (this.m22 * matrix.m23) + (this.m23 * matrix.m33) + (this.m24 * matrix.m43),
            (this.m21 * matrix.m14) + (this.m22 * matrix.m24) + (this.m23 * matrix.m34) + (this.m24 * matrix.m44),
            // Row 3
            (this.m31 * matrix.m11) + (this.m32 * matrix.m21) + (this.m33 * matrix.m31) + (this.m34 * matrix.m41),
            (this.m31 * matrix.m12) + (this.m32 * matrix.m22) + (this.m33 * matrix.m32) + (this.m34 * matrix.m42),
            (this.m31 * matrix.m13) + (this.m32 * matrix.m23) + (this.m33 * matrix.m33) + (this.m34 * matrix.m43),
            (this.m31 * matrix.m14) + (this.m32 * matrix.m24) + (this.m33 * matrix.m34) + (this.m34 * matrix.m44),
            // Row 4
            (this.m41 * matrix.m11) + (this.m42 * matrix.m21) + (this.m43 * matrix.m31) + (this.m44 * matrix.m41),
            (this.m41 * matrix.m12) + (this.m42 * matrix.m22) + (this.m43 * matrix.m32) + (this.m44 * matrix.m42),
            (this.m41 * matrix.m13) + (this.m42 * matrix.m23) + (this.m43 * matrix.m33) + (this.m44 * matrix.m43),
            (this.m41 * matrix.m14) + (this.m42 * matrix.m24) + (this.m43 * matrix.m34) + (this.m44 * matrix.m44),
            // Product of orthonormal 3D transformHACK matrices is also an orthonormal 3D transformHACK.
            this.isOrthonormalTransform && matrix.isOrthonormalTransform);
    }

    public final Matrix divideComponents(double value)
    {
        if (isZero(value))
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", value);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return new Matrix(
            this.m11 / value, this.m12 / value, this.m13 / value, this.m14 / value,
            this.m21 / value, this.m22 / value, this.m23 / value, this.m24 / value,
            this.m31 / value, this.m32 / value, this.m33 / value, this.m34 / value,
            this.m41 / value, this.m42 / value, this.m43 / value, this.m44 / value);
    }

    public final Matrix divideComponents(Matrix matrix)
    {
        if (matrix == null)
        {
            String msg = Logging.getMessage("nullValue.MatrixIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return new Matrix(
            this.m11 / matrix.m11, this.m12 / matrix.m12, this.m13 / matrix.m13, this.m14 / matrix.m14,
            this.m21 / matrix.m21, this.m22 / matrix.m22, this.m23 / matrix.m23, this.m24 / matrix.m24,
            this.m31 / matrix.m31, this.m32 / matrix.m32, this.m33 / matrix.m33, this.m34 / matrix.m34,
            this.m41 / matrix.m41, this.m42 / matrix.m42, this.m43 / matrix.m43, this.m44 / matrix.m44);
    }

    public final Matrix negate()
    {
        return new Matrix(
            0.0 - this.m11, 0.0 - this.m12, 0.0 - this.m13, 0.0 - this.m14,
            0.0 - this.m21, 0.0 - this.m22, 0.0 - this.m23, 0.0 - this.m24,
            0.0 - this.m31, 0.0 - this.m32, 0.0 - this.m33, 0.0 - this.m34,
            0.0 - this.m41, 0.0 - this.m42, 0.0 - this.m43, 0.0 - this.m44,
            // Negative of orthonormal 3D transformHACK matrix is also an orthonormal 3D transformHACK.
            this.isOrthonormalTransform);
    }

    // ============== Matrix Arithmetic Functions ======================= //
    // ============== Matrix Arithmetic Functions ======================= //
    // ============== Matrix Arithmetic Functions ======================= //

    public final double getDeterminant()
    {
        double result = 0.0;
        // Columns 2, 3, 4.
        result += this.m11 *
            (this.m22 * (this.m33 * this.m44 - this.m43 * this.m34)
                - this.m23 * (this.m32 * this.m44 - this.m42 * this.m34)
                + this.m24 * (this.m32 * this.m43 - this.m42 * this.m33));
        // Columns 1, 3, 4.
        result -= this.m12 *
            (this.m21 * (this.m33 * this.m44 - this.m43 * this.m34)
                - this.m23 * (this.m31 * this.m44 - this.m41 * this.m34)
                + this.m24 * (this.m31 * this.m43 - this.m41 * this.m33));
        // Columns 1, 2, 4.
        result += this.m13 *
            (this.m21 * (this.m32 * this.m44 - this.m42 * this.m34)
                - this.m22 * (this.m31 * this.m44 - this.m41 * this.m34)
                + this.m24 * (this.m31 * this.m42 - this.m41 * this.m32));
        // Columns 1, 2, 3.
        result -= this.m14 *
            (this.m21 * (this.m32 * this.m43 - this.m42 - this.m33)
                - this.m22 * (this.m31 * this.m43 - this.m41 * this.m33)
                + this.m23 * (this.m31 * this.m42 - this.m41 * this.m32));
        return result;
    }

    public final Matrix getTranspose()
    {
        // Swap rows with columns.
        return new Matrix(
            this.m11, this.m21, this.m31, this.m41,
            this.m12, this.m22, this.m32, this.m42,
            this.m13, this.m23, this.m33, this.m43,
            this.m14, this.m24, this.m34, this.m44,
            // Transpose of orthonormal 3D transformHACK matrix is not an orthonormal 3D transformHACK matrix.
            false);
    }

    public final double getTrace()
    {
        return this.m11 + this.m22 + this.m33 + this.m44;
    }

    public final Matrix getInverse()
    {
        if (this.isOrthonormalTransform)
            return computeTransformInverse(this);
        else
            return computeGeneralInverse(this);
    }
    
    private static Matrix computeGeneralInverse(Matrix a)
    {
        double cf_11 = a.m22 * (a.m33 * a.m44 - a.m43 * a.m34)
            - a.m23 * (a.m32 * a.m44 - a.m42 * a.m34)
            + a.m24 * (a.m32 * a.m43 - a.m42 * a.m33);
        double cf_12 = -(a.m21 * (a.m33 * a.m44 - a.m43 * a.m34)
            - a.m23 * (a.m31 * a.m44 - a.m41 * a.m34)
            + a.m24 * (a.m31 * a.m43 - a.m41 * a.m33));
        double cf_13 = a.m21 * (a.m32 * a.m44 - a.m42 * a.m34)
            - a.m22 * (a.m31 * a.m44 - a.m41 * a.m34)
            + a.m24 * (a.m31 * a.m42 - a.m41 * a.m32);
        double cf_14 = -(a.m21 * (a.m32 * a.m43 - a.m42 - a.m33)
            - a.m22 * (a.m31 * a.m43 - a.m41 * a.m33)
            + a.m23 * (a.m31 * a.m42 - a.m41 * a.m32));
        double cf_21 = a.m12 * (a.m33 * a.m44 - a.m43 - a.m34)
            - a.m13 * (a.m32 * a.m44 - a.m42 * a.m34)
            + a.m14 * (a.m32 * a.m43 - a.m42 * a.m33);
        double cf_22 = -(a.m11 * (a.m33 * a.m44 - a.m43 * a.m34)
            - a.m13 * (a.m31 * a.m44 - a.m41 * a.m34)
            + a.m14 * (a.m31 * a.m43 - a.m41 * a.m33));
        double cf_23 = a.m11 * (a.m32 * a.m44 - a.m42 * a.m34)
            - a.m12 * (a.m31 * a.m44 - a.m41 * a.m34)
            + a.m14 * (a.m31 * a.m42 - a.m41 * a.m32);
        double cf_24 = -(a.m11 * (a.m32 * a.m43 - a.m42 * a.m33)
            - a.m12 * (a.m31 * a.m43 - a.m41 * a.m33)
            + a.m13 * (a.m31 * a.m42 - a.m41 * a.m32));
        double cf_31 = a.m12 * (a.m23 * a.m44 - a.m43 * a.m24)
            - a.m13 * (a.m22 * a.m44 - a.m42 * a.m24)
            + a.m14 * (a.m22 * a.m43 - a.m42 * a.m23);
        double cf_32 = -(a.m11 * (a.m23 * a.m44 - a.m43 * a.m24)
            - a.m13 * (a.m21 * a.m44 - a.m41 * a.m24)
            + a.m14 * (a.m24 * a.m43 - a.m41 * a.m23));
        double cf_33 = a.m11 * (a.m22 * a.m44 - a.m42 * a.m24)
            - a.m12 * (a.m21 * a.m44 - a.m41 * a.m24)
            + a.m14 * (a.m21 * a.m42 - a.m41 * a.m22);
        double cf_34 = -(a.m11 * (a.m22 * a.m33 - a.m32 * a.m23)
            - a.m12 * (a.m21 * a.m33 - a.m31 * a.m23)
            + a.m13 * (a.m21 * a.m32 - a.m31 * a.m22));
        double cf_41 = a.m12 * (a.m23 * a.m34 - a.m33 * a.m24)
            - a.m13 * (a.m22 * a.m34 - a.m32 * a.m24)
            + a.m14 * (a.m22 * a.m33 - a.m32 * a.m23);
        double cf_42 = -(a.m11 * (a.m23 * a.m34 - a.m33 * a.m24)
            - a.m13 * (a.m21 * a.m34 - a.m31 * a.m24)
            + a.m14 * (a.m21 * a.m33 - a.m31 * a.m23));
        double cf_43 = a.m11 * (a.m22 * a.m34 - a.m32 * a.m24)
            - a.m12 * (a.m21 * a.m34 - a.m31 * a.m24)
            + a.m14 * (a.m21 * a.m32 - a.m31 * a.m22);
        double cf_44 = -(a.m11 * (a.m22 * a.m33 - a.m32 * a.m23)
            - a.m12 * (a.m21 * a.m33 - a.m31 * a.m23)
            + a.m13 * (a.m21 * a.m32 - a.m31 * a.m22));
        double det = (a.m11 * cf_11) + (a.m12 * cf_12) + (a.m13 * cf_13) + (a.m14 * cf_14);

        if (isZero(det))
            return null;
        return new Matrix(
            cf_11 / det, cf_21 / det, cf_31 / det, cf_41 / det,
            cf_12 / det, cf_22 / det, cf_32 / det, cf_42 / det,
            cf_13 / det, cf_23 / det, cf_33 / det, cf_43 / det,
            cf_14 / det, cf_24 / det, cf_34 / det, cf_44 / det);
    }

    private static Matrix computeTransformInverse(Matrix a)
    {
        // 'a' is assumed to contain a 3D transformation matrix.
        // Upper-3x3 is inverted, translation is transformed by inverted-upper-3x3 and negated.
        return new Matrix(
            a.m11, a.m21, a.m31, 0.0 - (a.m11 * a.m14) - (a.m21 * a.m24) - (a.m31 * a.m34),
            a.m12, a.m22, a.m32, 0.0 - (a.m12 * a.m14) - (a.m22 * a.m24) - (a.m32 * a.m34),
            a.m13, a.m23, a.m33, 0.0 - (a.m13 * a.m14) - (a.m23 * a.m24) - (a.m33 * a.m34),
            0.0, 0.0, 0.0, 1.0,
            // Inverse of an orthogonal, 3D transformHACK matrix is not an orthogonal 3D transformHACK.
            false);
    }

    // ============== Accessor Functions ======================= //
    // ============== Accessor Functions ======================= //
    // ============== Accessor Functions ======================= //
    
    public final Angle getRotationX()
    {
        double yRadians = Math.asin(this.m13);
        double cosY = Math.cos(yRadians);
        if (isZero(cosY))
            return null;

        double xRadians;
        // No Gimball lock.
        if (Math.abs(cosY) > 0.005)
        {
            xRadians = Math.atan2(-this.m23 / cosY, this.m33 / cosY);
        }
        // Gimball lock has occurred. Rotation around X axis becomes rotation around Z axis.
        else
        {
            xRadians = 0;
        }

        if (Double.isNaN(xRadians))
            return null;

        return Angle.fromRadians(xRadians);
    }

    public final Angle getRotationY()
    {
        double yRadians = Math.asin(this.m13);
        if (Double.isNaN(yRadians))
            return null;
        
        return Angle.fromRadians(yRadians);
    }

    public final Angle getRotationZ()
    {
        double yRadians = Math.asin(this.m13);
        double cosY = Math.cos(yRadians);
        if (isZero(cosY))
            return null;

        double zRadians;
        // No Gimball lock.
        if (Math.abs(cosY) > 0.005)
        {
            zRadians = Math.atan2(-this.m12 / cosY, this.m11 / cosY);
        }
        // Gimball lock has occurred. Rotation around X axis becomes rotation around Z axis.
        else
        {
            zRadians = Math.atan2(this.m21, this.m22);
        }

        if (Double.isNaN(zRadians))
            return null;

        return Angle.fromRadians(zRadians);
    }

    public final Vec4 getTranslation()
    {
        return new Vec4(this.m14, this.m24, this.m34);
    }

    // ============== Helper Functions ======================= //
    // ============== Helper Functions ======================= //
    // ============== Helper Functions ======================= //

    private static final Double POSITIVE_ZERO = +0.0d;

    private static final Double NEGATIVE_ZERO = -0.0d;

    private static boolean isZero(double value)
    {
        return (POSITIVE_ZERO.compareTo(value) == 0)
            || (NEGATIVE_ZERO.compareTo(value) == 0);
    }
}
