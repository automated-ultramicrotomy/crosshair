import sympy as sy
import numpy as np

# angle of sample tilt
qt = sy.symbols('qt', real=True)
# angle of sample rotation
qr = sy.symbols('qr', real=True)
# angle of knife tilt
qk = sy.symbols('qk', real=True)
# angle of initial knife tilt (on alignment)
qik = sy.symbols('qik', real=True)
# angle of initial sample tilt (on alignment)
qit = sy.symbols('qit', real=True)
# Block face to target plane z axis rotation - 'target offset'
to = sy.symbols('to', real=True)
# Block face to target plane x axis rotation - 'target rotation'
tr = sy.symbols('tr', real=True)

# CONSTANTS:
A = sy.symbols('A', real=True)
B = sy.symbols('B', real=True)
C = sy.symbols('C', real=True)
D = sy.symbols('D', real=True)
E = sy.symbols('E', real=True)
F = sy.symbols('F', real=True)
G = sy.symbols('G', real=True)
H = sy.symbols('H', real=True)
I = sy.symbols('I', real=True)


# Functions to make rotation matrices about x, y and z
def Rx(angle):
    """Make a rotation matrix for rotation about the x axis by angle"""
    return sy.Matrix([[1, 0, 0],
                      [0, sy.cos(angle), -sy.sin(angle)],
                      [0, sy.sin(angle), sy.cos(angle)]])


def Ry(angle):
    """Make a rotation matrix for rotation about the y axis by angle"""
    return sy.Matrix([[sy.cos(angle), 0, sy.sin(angle)],
                      [0, 1, 0],
                      [-sy.sin(angle), 0, sy.cos(angle)]])


def Rz(angle):
    """Make a rotation matrix for rotation about the z axis by angle"""
    return sy.Matrix([[sy.cos(angle), -sy.sin(angle), 0],
                      [sy.sin(angle), sy.cos(angle), 0],
                      [0, 0, 1]])


def expression_at_setup(exp, i_tilt, i_knife, t_offset, t_rotation):
    """
    Evaluate expression given the input angles
    :param exp: an expression
    :param i_tilt: initial tilt angle (of arc) in degrees
    :param i_knife: initial knife angle in degrees
    :param t_offset: target offset angle in degrees
    :param t_rotation: target rotation angle in degrees
    """
    exp = exp.subs(qit, np.radians(i_tilt))
    exp = exp.subs(qik, np.radians(i_knife))
    exp = exp.subs(to, np.radians(t_offset))
    exp = exp.subs(tr, np.radians(t_rotation))
    return exp


def full_expression(exp):
    """Function to turn an equation in terms of A-I constants, into the full expression in terms
    of qik, qit, to, tr..."""
    exp = exp.subs(A, sy.cos(qik + to))
    exp = exp.subs(B, sy.sin(tr) * sy.sin(qik + to))
    exp = exp.subs(C, sy.sin(qit) * sy.sin(qik + to))
    exp = exp.subs(D, sy.cos(qit) * sy.sin(qik + to))
    exp = exp.subs(E, sy.cos(tr) * sy.sin(qik + to))
    exp = exp.subs(F, sy.sin(qit) * sy.cos(tr))
    exp = exp.subs(G, sy.sin(tr) * sy.cos(qit))
    exp = exp.subs(H, sy.sin(qit) * sy.sin(tr))
    exp = exp.subs(I, sy.cos(qit) * sy.cos(tr))
    return exp

def expression_in_constants(exp):
    """Make an equation in terms of qit, qit etc... into one in terms of the constants A-I"""
    exp = exp.subs(sy.cos(qik + to), A)
    exp = exp.subs(sy.sin(tr) * sy.sin(qik + to), B)
    exp = exp.subs(sy.sin(qit) * sy.sin(qik + to), C)
    exp = exp.subs(sy.cos(qit) * sy.sin(qik + to), D)
    exp = exp.subs(sy.cos(tr) * sy.sin(qik + to), E)
    exp = exp.subs(sy.sin(qit) * sy.cos(tr), F)
    exp = exp.subs(sy.sin(tr) * sy.cos(qit), G)
    exp = exp.subs(sy.sin(qit) * sy.sin(tr), H)
    exp = exp.subs(sy.cos(qit) * sy.cos(tr), I)

    return exp


def main():
    # Full forward kinematics equation from world ref. frame to target ref. frame
    forward_kin = Rx(qt) * Ry(qr) * Rx(-qit) * Rz(qik) * Rz(to) * Rx(tr)

    # CALCULATION OF INVERSE KINEMATICS FOR ANY VERTICAL TARGET PLANE---------------------

    local_y_axis = sy.Matrix([0, 1, 0])
    global_z_axis = sy.Matrix([0, 0, 1])

    # local y axis in global coordinates
    l = forward_kin * local_y_axis
    # dot product between the axis above and the global z axis
    l_dot = l.dot(global_z_axis)
    # This must be equal to 0 for the target plane to be vertical i.e. for the
    # local y axis to be perpendicular to global z

    # simplification to make expression nicer
    simpl = sy.expand(sy.trigsimp(l_dot))
    simpl = sy.collect(simpl, [sy.sin(qt), sy.cos(qr) * sy.cos(qt)])

    # Now we can can collect some terms as constants - as qit / qik / to / tr are constants for a particular run
    # determined at the alignment step or from the x-ray
    simpl = expression_in_constants(simpl)
    print(f"Rearranged IK for vertical target plane: {simpl}")

    # So now simpl = 0
    # I solved this manually by taking sin(qt) over to the other side and dividing through
    # by cos(qt)

    # This gives the following solution with the following constants:
    solution_tilt = sy.atan(((-A * F + G) / (-A * I - H)) * sy.cos(qr) + ((E / (-A * I - H)) * sy.sin(qr)))
    print(f"Final solution for qt: {solution_tilt}")

    # Values for C1; C2; C3 constants as described in lab-book
    # C1 = (sy.sin(qit) * sy.sin(tr) + sy.cos(qit) * sy.cos(tr) * sy.cos(qik + to))
    # C2 = (-sy.sin(qit) * sy.cos(tr) * sy.cos(qik + to) + sy.sin(tr) * sy.cos(qit))
    # C3 = sy.sin(qik + to) * sy.cos(tr)
    #
    # # Values for A and B constants
    # A = C2 / -C1
    # B = C3 / -C1
    #
    # # SOLUTION: i.e. qt = atan(Acos(qr) + Bsin(qr))
    #
    # solution = sy.atan(A * sy.cos(qr) + B * sy.sin(qr))

    # TESTING OF SOLUTION-----------------------------------------------------------

    # Evaluate at example values for one of my targeting attempts

    # Values of constants at specific values
    # A_spec = expression_at_setup(A, 10, 10, -3.264347791671753, 5.367281913757324)
    # B_spec = expression_at_setup(B, 10, 10, -3.264347791671753, 5.367281913757324)
    #
    # # Plot of qr vs qt for specific values above
    # sy.plot(sy.atan(A_spec * sy.cos(qr) + B_spec * sy.sin(qr)), (qr, -sy.pi, sy.pi), xlabel='qr', ylabel='qt')
    #
    # # Plot in degrees
    # graph = sy.plot(
    #     (sy.atan(A_spec * sy.cos(qr * (sy.pi / 180)) + B_spec * sy.sin(qr * (sy.pi / 180)))) * (180 / sy.pi),
    #     (qr, -180, 180), xlabel='rotation (deg)', ylabel='tilt (deg)', show=False)
    # graph.show()
    # # graph._backend.fig.savefig('C:\\Users\\meechan\\Documents\\EMBL_group\\Presentations\\14-01-19-EMCF Meeting\\plot.png', dpi = 300)
    #
    # # Shape for different A & B values
    # sy.plot(sy.atan(0.5 * sy.cos(qr) + -0.5 * sy.sin(qr)), (qr, -sy.pi, sy.pi), xlabel='qr', ylabel='qt')

    # SIMPLIFYING LATER STEPS-----------------------------------------------------------------

    # To make some of the later steps simpler, we can instead introduce a series of constants
    # into the forward kinematics equation itself. This gives the same solution, but with
    # different constants

    # Full forward kinematics equation from world ref. frame to target ref. frame
    # Forward_kin = Rx(qt) * Ry(qr) * Rx(-qit) * Rz(qik) * Rz(to) * Rx(tr)
    # # Simplify and expand the expressions in the matrix
    # FK = sy.expand(sy.trigsimp(Forward_kin))
    # # Sub in constants in place of the following values:
    # FK = FK.subs(sy.cos(qik + to), A)
    # FK = FK.subs(sy.sin(tr) * sy.sin(qik + to), B)
    # FK = FK.subs(sy.sin(qit) * sy.sin(qik + to), C)
    # FK = FK.subs(sy.cos(qit) * sy.sin(qik + to), D)
    # FK = FK.subs(sy.cos(tr) * sy.sin(qik + to), E)
    # FK = FK.subs(sy.sin(qit) * sy.cos(tr), F)
    # FK = FK.subs(sy.sin(tr) * sy.cos(qit), G)
    # FK = FK.subs(sy.sin(qit) * sy.sin(tr), H)
    # FK = FK.subs(sy.cos(qit) * sy.cos(tr), I)

    # FK is now the same matrix, but the only sin/cos are of qr & qt, the variables
    # we actually care about

    # Now we solve in the same way as before:
    # local_y_axis = sy.Matrix([0, 1, 0])
    # global_z_axis = sy.Matrix([0, 0, 1])
    #
    # local y axis in global coordinates
    # l = FK * local_y_axis
    # dot product between the axis above and the global z axis
    # l_dot = l.dot(global_z_axis)
    # This must be equal to 0 for the target plane to be vertical i.e. for the
    # local y axis to be perpendicular to global z

    # simplification to make expression nicer
    # simpl = sy.collect(l_dot, [sy.sin(qt), sy.cos(qr) * sy.cos(qt)])

    # So now simpl = 0, solve in same way as before to give:
    # solution2 = sy.atan(((-A * F + G) / (-A * I - H)) * sy.cos(qr) + ((E / (-A * I - H)) * sy.sin(qr)))

    ##We can check this is exactly the same as the previous solution as follows:
    ##set values of constants to full expression & check equal:

    # print(full_expression(solution2) == solution)

    # CALCULATING CORRESPONDING KNIFE VALUE--------------------------------------------------
    # For this I will use the second version of the solution

    # The signed value of the angle between global y and local y (i.e. the required
    # knife rotation) is equal to atan([(global_y X local_y).dot(global_z)] / (global_y.dot(local_y)))
    # as per the solution here:https://stackoverflow.com/questions/5188561/signed-angle-between-two-3d-vectors-with-same-origin-within-the-same-plane

    # global y axis
    global_y_axis = sy.Matrix([0, 1, 0])

    # local y axis in global coordinates
    l = forward_kin * local_y_axis

    top_eq = (global_y_axis.cross(l)).dot(global_z_axis)
    top_eq = sy.expand(sy.trigsimp(top_eq))
    top_eq = sy.collect(top_eq, [sy.sin(qr)])

    bot_eq = global_y_axis.dot(l)
    bot_eq = sy.expand(sy.trigsimp(bot_eq))
    bot_eq = sy.collect(bot_eq, [sy.sin(qt) * sy.cos(qr), sy.sin(qr) * sy.sin(qt), sy.cos(qt)])

    knife_sol = sy.atan(top_eq/bot_eq)
    knife_sol = expression_in_constants(knife_sol)

    knife_sol = knife_sol.subs(qt, solution_tilt)
    knife_sol = sy.simplify(knife_sol)



    bot_eq = sy.collect(bot_eq, [sy.sin(qt) * sy.cos(qr), sy.sin(qr) * sy.sin(qt), sy.cos(qt)])

    # This gives us the signed angle for all values of qr/qt, but we only want it for valid
    # solutions as calculated above
    # Therefore, we sub the solution in for qt in the bottom equation (the only one with qt in it)
    bot_eq = bot_eq.subs(qt, solution_tilt)

    knife_sol = sy.atan(top_eq / bot_eq)
    knife_sol = sy.simplify(knife_sol)
    knife_sol = sy.expand(knife_sol)
    knife_sol = expression_in_constants(knife_sol)
    knife_sol = sy.simplify(knife_sol)


    knife_sol = sy.expand(knife_sol)

    top_eq = (global_y_axis.cross(l)).dot(global_z_axis)
    top_eq = sy.collect(top_eq, [sy.sin(qr)])
    bot_eq = global_y_axis.dot(l)
    bot_eq = sy.expand(sy.trigsimp(bot_eq))
    bot_eq = sy.collect(bot_eq, [sy.sin(qt) * sy.cos(qr), sy.sin(qr) * sy.sin(qt), sy.cos(qt)])

    top_eq = expression_in_constants(top_eq)
    bot_eq = expression_in_constants(bot_eq)


    bot_eq.subs(qt, solution_tilt)

    knife_sol = sy.atan(top_eq / bot_eq)
    knife_sol = expression_in_constants(knife_sol)
    # knife_sol.subs(qt, solution_tilt)
    knife_sol = sy.simplify(knife_sol)
    knife_sol = expression_in_constants(knife_sol)

    knife_sol = knife_sol.subs(qt, solution_tilt)
    knife_sol = sy.simplify(knife_sol)

    # This gives us the signed angle for all values of qr/qt, but we only want it for valid
    # solutions as calculated above
    # Therefore, we sub the solution in for qt in the bottom equation (the only one with qt in it)
    bot_eq = bot_eq.subs(qt, solution_tilt)

    knife_sol = sy.atan(top_eq / bot_eq)
    knife_sol = sy.simplify(knife_sol)
    knife_sol = expression_in_constants(knife_sol)

    # TESTING OF SOLUTION--------------------------------------------------------

    # Calculate solution for specific values
    full_knife_sol = full_expression(knife_sol)
    full_setup = expression_at_setup(full_knife_sol, 10, 10, -3.264347791671753, 5.367281913757324)
    sy.plot(full_setup, (qr, -sy.pi, sy.pi), xlabel='qr', ylabel='qk')

    # plot in degrees
    deg_sim = full_setup.subs(qr, qr * (sy.pi / 180))
    graph2 = sy.plot(deg_sim * (180 / sy.pi), (qr, -180, 180), xlabel='rotation (deg)', ylabel='knife (deg)',
                     line_color='red', show=False)
    graph2.show()
    graph2._backend.fig.savefig(
        'C:\\Users\\meechan\\Documents\\EMBL_group\\Presentations\\14-01-19-EMCF Meeting\\knife.png', dpi=300)

    # plot of general solution vs knife solution on top of eachother
    graph_copy = graph2
    graph.append(graph_copy[0])
    graph.show()
    graph._backend.fig.savefig(
        'C:\\Users\\meechan\\Documents\\EMBL_group\\Presentations\\14-01-19-EMCF Meeting\\knifevsfull.png', dpi=300)

    # UNSIGNED VERSION OF KNIFE ANGLE SOLUTION---------------------------------------------------------

    # local y axis
    l = Forward_kin * local_y_axis

    # global y axis
    global_y_axis = sy.Matrix([0, 1, 0])

    # Absolute angle between local y axis (in world coordinates) and global y axis
    # Calculated in standard way from dot product equation
    qk = sy.acos(l.dot(global_y_axis) / sy.sqrt(l[0, 0] ** 2 + l[1, 0] ** 2 + l[2, 0] ** 2))

    # Want only values of qk that also satisfy the solution above
    # Eliminate qt by subbing in the solution above
    simmed = qk.subs(qt, solution)

    # TESTING OF SOLUTION--------------------------------------------------------

    # Calculate solution for specific values
    full_simmed = expression_at_setup(simmed, 10, 10, -3.264347791671753, 5.367281913757324)
    sy.plot(full_simmed, (qr, -sy.pi, sy.pi), xlabel='qr', ylabel='qk')

    # plot in degrees
    deg_sim = full_simmed.subs(qr, qr * (sy.pi / 180))
    graph3 = sy.plot(deg_sim * (180 / sy.pi), (qr, -180, 180), xlabel='rotation (deg)', ylabel='knife (deg)',
                     show=False)
    graph3.show()
    # graph2._backend.fig.savefig('C:\\Users\\meechan\\Documents\\EMBL_group\\Presentations\\14-01-19-EMCF Meeting\\knife.png', dpi = 300)

    # compare signed and unsigned solution
    graph_copy = graph3
    graph2.append(graph_copy[0])
    graph2.show()


if __name__ == "__main__":
    main()



