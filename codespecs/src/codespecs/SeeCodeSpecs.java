package codespecs;

/**
 * Refers to code specs for the annotated element.
 *  
 * When applied to a package x.y.z, indicates that code specs for x.y.z are in package x.y.z.spec.
 * When applied to a class x.y.Z, indicates that code specs for x.y.Z are in class x.y.ZSpec.
 * When applied to a constructor x.y.Z.Z, indicates that method x.y.Z.constructorSpec is a code spec for x.y.Z.Z.
 * When applied to a method x.y.Z.m, indicates that method x.y.Z.mSpec is a code spec for x.y.Z.m.
 *  
 * CodeSpecsWeaver weaves code that checks these specs into the specified constructors and methods.
 * It also weaves checks at dynamically bound call sites.
 */
public @interface SeeCodeSpecs {
}
