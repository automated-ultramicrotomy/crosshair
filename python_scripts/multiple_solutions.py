import matplotlib.pyplot as plt
import sympy as sy
import numpy as np
import os

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


def save_graph(graph, save_dir, name):
    """Save a sympy graph"""
    backend = graph.backend(graph)
    backend.process_series()
    backend.fig.savefig(os.path.join(save_dir, name), dpi=300)


def plot_graphs(solution_tilt, solution_knife, i_tilt, i_knife, t_offset, t_rotation, save_dir):
    """
    Plot graphs of solution_tilt and solution_knife equations
    :param solution_tilt: tilt solution equation
    :param solution_knife: knife solution equation
    :param i_tilt: initial tilt angle (of arc) in degrees
    :param i_knife: initial knife angle in degrees
    :param t_offset: target offset angle in degrees
    :param t_rotation: target rotation angle in degrees
    :param save_dir: path to directory to save graphs in
    """
    # Calculate tilt solution for specific values
    full_tilt_sol = full_expression(solution_tilt)
    full_setup_tilt = expression_at_setup(full_tilt_sol, i_tilt, i_knife, t_offset, t_rotation)
    tilt_graph_radians = sy.plot(full_setup_tilt, (qr, -sy.pi, sy.pi), xlabel="r", ylabel="t", show=False)
    save_graph(tilt_graph_radians, save_dir, "tilt_solution_radians.png")

    # Calculate knife solution for specific values
    full_knife_sol = full_expression(solution_knife)
    full_setup_knife = expression_at_setup(full_knife_sol, i_tilt, i_knife, t_offset, t_rotation)
    knife_graph_radians = sy.plot(full_setup_knife, (qr, -sy.pi, sy.pi), line_color='red', xlabel="r",
                                  ylabel="k", show=False)
    save_graph(knife_graph_radians, save_dir, "knife_solution_radians.png")

    # plot in degrees
    full_setup_tilt_degrees = full_setup_tilt.subs(qr, qr * (sy.pi / 180))
    tilt_graph_degrees = sy.plot(full_setup_tilt_degrees * (180 / sy.pi), (qr, -180, 180), xlabel='r',
                                 ylabel='t', show=False)
    save_graph(tilt_graph_degrees, save_dir, "tilt_solution_degrees.png")

    full_setup_knife_degrees = full_setup_knife.subs(qr, qr * (sy.pi / 180))
    knife_graph_degrees = sy.plot(full_setup_knife_degrees * (180 / sy.pi), (qr, -180, 180),
                                  line_color='red', xlabel="r", ylabel="k", show=False)
    save_graph(knife_graph_degrees, save_dir, "knife_solution_degrees.png")

    # all graphs together
    graph_copy = knife_graph_degrees
    tilt_graph_degrees.append(graph_copy[0])
    save_graph(tilt_graph_degrees, save_dir, "combined_solutions_degrees.png")


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

    # simplification of expression
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

    # CALCULATING CORRESPONDING KNIFE VALUE--------------------------------------------------

    # The signed value of the angle between global y and local y (i.e. the required
    # knife rotation) is equal to atan([(global_y X local_y).dot(global_z)] / (global_y.dot(local_y)))
    # as per the solution here:
    # https://stackoverflow.com/questions/5188561/signed-angle-between-two-3d-vectors-with-same-origin-within-the-same-plane

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

    solution_knife = sy.atan(top_eq/bot_eq)
    print(f"Rearranged knife equation: {solution_knife}")
    solution_knife = expression_in_constants(solution_knife)
    print(f"Rearranged knife equation in constants: {solution_knife}")

    solution_knife = solution_knife.subs(qt, solution_tilt)
    solution_knife = sy.simplify(solution_knife)
    print(f"Simplified knife equation with subbed tilt solution: {solution_knife}")

    # TESTING OF SOLUTION--------------------------------------------------------

    # example plots with specific values e.g.
    i_tilt = 10
    i_knife = 10
    t_offset = -3.3
    t_rotation = 5.4

    plot_graphs(solution_tilt, solution_knife, i_tilt, i_knife, t_offset, t_rotation,
                "C:\\Users\\meechan\\Documents\\Repos\\thesis\\figures\\Chapter2_drafts\\solution_graphs\\one")

    # example plots with extreme values e.g.
    i_tilt = 10
    i_knife = 10
    t_offset = -60
    t_rotation = 5.4

    plot_graphs(solution_tilt, solution_knife, i_tilt, i_knife, t_offset, t_rotation,
                "C:\\Users\\meechan\\Documents\\Repos\\thesis\\figures\\Chapter2_drafts\\solution_graphs\\two")


if __name__ == "__main__":
    main()



