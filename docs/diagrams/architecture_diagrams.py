#!/usr/bin/env python3
# /// script
# requires-python = ">=3.10"
# dependencies = ["diagrams>=0.24"]
# ///
"""Draw the Franquix architecture diagrams next to this script.

    uv run docs/diagrams/architecture_diagrams.py

Writes two PNGs to ``docs/diagrams/``:

- ``franquix-deployment.png`` — GitHub Actions, Render, MongoDB Atlas and Terraform (README "Deploy to Render").
- ``franquix-hexagonal.png`` — the layers and ports inside the API (README "Architecture").

Needs Graphviz (``winget install Graphviz.Graphviz`` on Windows, ``brew install graphviz`` on macOS);
``uv`` installs the ``diagrams`` package on the fly from the inline metadata above.
"""

from __future__ import annotations

import os
import shutil
from pathlib import Path

OUT_DIR = Path(__file__).resolve().parent

# The Graphviz installer on Windows doesn't always put ``dot`` on PATH.
_WINDOWS_GRAPHVIZ = Path(r"C:\Program Files\Graphviz\bin")
if shutil.which("dot") is None and _WINDOWS_GRAPHVIZ.is_dir():
    os.environ["PATH"] = f"{_WINDOWS_GRAPHVIZ}{os.pathsep}{os.environ['PATH']}"

from diagrams import Cluster, Diagram, Edge  # noqa: E402  (needs PATH above)
from diagrams.onprem.ci import GithubActions  # noqa: E402
from diagrams.onprem.client import Users  # noqa: E402
from diagrams.onprem.container import Docker  # noqa: E402
from diagrams.onprem.database import MongoDB  # noqa: E402
from diagrams.onprem.iac import Terraform  # noqa: E402
from diagrams.onprem.vcs import Github  # noqa: E402
from diagrams.programming.framework import Spring  # noqa: E402
from diagrams.programming.language import Java  # noqa: E402

FONT = "Arial"

GRAPH = {
    "fontname": FONT,
    "fontsize": "34",
    "labelloc": "t",
    "pad": "0.8",
    # Wide gaps: node labels are wider than the fixed 1.4" icon box and would overlap their neighbours.
    "nodesep": "1.3",
    "ranksep": "2.0",
    "bgcolor": "white",
    # diagrams defaults to "ortho", which detaches edge labels from their edges.
    "splines": "spline",
}
# Icon pinned to the top of its box: centred (the default) it covers the first line of a multi-line label.
NODE = {"fontname": FONT, "fontsize": "20", "imagepos": "tc"}
EDGE = {"fontname": FONT, "fontsize": "18", "color": "#232F3E", "fontcolor": "#232F3E"}

# Every Edge(...) re-applies these class defaults (fontsize 13) over the diagram's edge_attr.
Edge._default_edge_attrs = {**Edge._default_edge_attrs, "fontname": FONT, "fontsize": EDGE["fontsize"]}


def frame(color: str, fill: str = "white", dashed: bool = False) -> dict[str, str]:
    return {
        "fontname": FONT,
        "fontsize": "22",
        "fontcolor": color,
        "pencolor": color,
        "bgcolor": fill,
        "style": "dashed,rounded" if dashed else "rounded",
        "penwidth": "1.8",
        # Labels overflow the fixed icon box sideways; the margin keeps them inside the frame.
        "margin": "48",
        "labeljust": "l",
    }


GITHUB = frame("#24292F", fill="#F6F8FA")
RENDER = frame("#4351E8", fill="#EEF0FD")
ATLAS = frame("#00684A", fill="#E8F5EF")
AWS_REGION = frame("#00A4A6", dashed=True)

DOMAIN = frame("#B45309", fill="#FEF5E7")
APPLICATION = frame("#4351E8", fill="#EEF0FD")
INFRASTRUCTURE = frame("#00684A", fill="#E8F5EF")


def flow(label: str = "") -> Edge:
    return Edge(label=label, color="#232F3E", penwidth="2")


def secondary(label: str = "") -> Edge:
    return Edge(label=label, color="#7D8998", fontcolor="#5A6573", style="dashed")


def implements() -> Edge:
    """Dotted arrow, UML-style: the adapter or service implements the port."""
    return Edge(label="implements", color="#7D8998", fontcolor="#5A6573", style="dotted", arrowhead="onormal")


def deployment() -> None:
    with Diagram(
        "Franquix — deployment",
        filename=str(OUT_DIR / "franquix-deployment"),
        outformat="png",
        show=False,
        direction="LR",
        graph_attr=GRAPH,
        node_attr=NODE,
        edge_attr=EDGE,
    ):
        client = Users("API client\nAuthorization: Bearer <API_TOKEN>")

        with Cluster("GitHub · SantiagoRuizM/franquix", graph_attr=GITHUB):
            repo = Github("git push main")
            ci = GithubActions("CI · ci.yml\ngradle build · JaCoCo ≥ 80 %\nterraform fmt + validate")

        with Cluster("Render · free plan · virginia", graph_attr=RENDER):
            api = Docker("franquix-api\nDocker image\nsleeps after 15 min idle")
            app = Spring("Spring Boot 3.5 WebFlux\nJava 21 · port 8080\n/actuator/health")

        tf = Terraform("Terraform\ninfra/envs/prod")

        with Cluster("MongoDB Atlas · project franquix", graph_attr=ATLAS):
            with Cluster("AWS us-east-1", graph_attr=AWS_REGION):
                mongo = MongoDB("M0 shared cluster\ndatabase franquix\nfranchises · branches · products")

        repo >> flow() >> ci >> flow("checks pass →\nauto-deploy") >> api
        client >> flow("HTTPS") >> api
        api >> Edge(color="#4351E8", style="dotted", arrowhead="none") >> app
        app >> flow("MONGODB_URI · TLS\nreadWrite user") >> mongo
        tf >> secondary("provisions cluster, user,\nIP access list 0.0.0.0/0") >> mongo


def hexagonal() -> None:
    with Diagram(
        "Franquix — hexagonal architecture",
        filename=str(OUT_DIR / "franquix-hexagonal"),
        outformat="png",
        show=False,
        direction="LR",
        graph_attr=GRAPH,
        node_attr=NODE,
        edge_attr=EDGE,
    ):
        client = Users("API client")
        database = MongoDB("MongoDB")

        with Cluster("infrastructure · entrypoint", graph_attr=INFRASTRUCTURE):
            token = Spring("ApiTokenFilter\nbearer token on /api/**")
            router = Spring("FranchiseRouter\n+ Franchise / Branch / Product\nHandlers · DTOs")
            errors = Spring("GlobalErrorHandler\nRFC 7807 ProblemDetail")

        with Cluster("application", graph_attr=APPLICATION):
            services = Java("FranchiseService\nBranchService\nProductService")

        with Cluster("domain · no Spring, no MongoDB (ArchUnit)", graph_attr=DOMAIN):
            ports_in = Java("port/in\nFranchiseUseCase · BranchUseCase\nProductUseCase")
            model = Java("model\nFranchise · Branch · Product\nimmutable records")
            ports_out = Java("port/out\nFranchise / Branch / Product\nRepositoryPort")

        with Cluster("infrastructure · adapter", graph_attr=INFRASTRUCTURE):
            adapters = Spring("Mongo*RepositoryAdapter\nreactive repositories\ndocuments + unique indexes")

        client >> flow("HTTP") >> token >> flow() >> router >> flow("calls") >> ports_in
        router >> secondary("errors") >> errors
        services >> implements() >> ports_in
        services >> flow("uses") >> model
        services >> flow("uses") >> ports_out
        adapters >> implements() >> ports_out
        adapters >> flow("Mono / Flux") >> database


def main() -> None:
    deployment()
    hexagonal()
    for png in sorted(OUT_DIR.glob("franquix-*.png")):
        print(png.name)


if __name__ == "__main__":
    main()
