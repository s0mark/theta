package hu.bme.mit.theta.solver.smtlib;

import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverFactory;

import java.nio.file.Path;

public class SmtLibSolverFactory implements SolverFactory {
    private final Path solverPath;
    private final String[] args;

    private SmtLibSolverFactory(Path solverPath, String[] args) {
        this.solverPath = solverPath;
        this.args = args;
    }

    public static SmtLibSolverFactory create(Path solverPath, String[] args) {
        return new SmtLibSolverFactory(solverPath, args);
    }

    @Override
    public Solver createSolver() {
        final var symbolTable = new SmtLibSymbolTable();
        final var transformationManager = new SmtLibTransformationManager(symbolTable);
        final var termTransformer = new SmtLibTermTransformer(symbolTable);

        return new SmtLibSolver(symbolTable, transformationManager, termTransformer, solverPath, args);
    }

    @Override
    public ItpSolver createItpSolver() {
        throw new UnsupportedOperationException("SmtLibSolver does not support interpolation");
    }
}