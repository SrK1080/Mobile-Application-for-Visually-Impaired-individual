%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% A UCC CS Thesis Style
% By M.R.C. van Dongen.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{uccthesis}[2010/03/17 UCC CS Thesis Style]

\makeatletter

\usepackage[a4paper]{geometry}
\usepackage{uccutil1}

\DeclareOption{10pt}{\def\ucc@x@pt{10pt}}
\DeclareOption{11pt}{\def\ucc@x@pt{11pt}}
\DeclareOption{12pt}{\def\ucc@x@pt{12pt}}

\DeclareOption{oneside}{\def\ucc@x@side{oneside}}
\DeclareOption{twoside}{\def\ucc@x@side{twoside}}

\DeclareOption{ManyFigures}{\AtBeginDocument{\renewcommand*{\l@figure}{\@dottedtocline{1}{1.4em}{2.2em}}}}
\DeclareOption{ManyTables}{\AtBeginDocument{\renewcommand*{\l@table}{\@dottedtocline{1}{1.4em}{2.2em}}}}
\DeclareOption{ManySections}{\AtBeginDocument{\renewcommand*{\l@section}{\@dottedtocline{1}{1.4em}{2.2em}}}}
\DeclareOption{ManySubsections}{\AtBeginDocument{\renewcommand*{\l@subsection}{\@dottedtocline{1}{3.7em}{2.8em}}}}

\newif\ifucc@phd
\ucc@phdfalse
% TO DO Add names of external examiners \ifucc@phd...
\DeclareOption{MScResearch}{\def\ucc@degree{MSc in Computer Science (Research)}}
\DeclareOption{MScIM}{\def\ucc@degree{MSc Interactive Media}}
\DeclareOption{MScCS}{\def\ucc@degree{MSc Computing Science}}
\DeclareOption{MScDSA}{\def\ucc@degree{MSc Data Science and Analytics}}
\DeclareOption{PhD}{\def\ucc@degree{PhD Computer Science}\ucc@phdtrue}
\DeclareOption{BScCS}{\def\ucc@degree{BSc Computing Science}}
\DeclareOption{BScDSA}{\def\ucc@degree{BSc Data Science and Analytics}}
\DeclareOption{DHIT}{\def\ucc@degree{BA Digital Humanities and Information Technologies}}

\DeclareOption{final}{\ucc@draftfalse}
\DeclareOption{draft}{\ucc@drafttrue}

\DeclareOption{lot}{\ucc@with@lottrue}
\DeclareOption{nolot}{\ucc@with@lotfalse}
\DeclareOption{nolof}{\ucc@with@loffalse}
\DeclareOption{lof}{\ucc@with@loftrue}

\DeclareOption{singlespacing}{\def\ucc@spacing{singlespacing}}
\DeclareOption{onehalfspacing}{\def\ucc@spacing{onehalfspacing}}
\DeclareOption{doublespacing}{\def\ucc@spacing{doublespacing}}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Disallowed options
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
\def\ucc@declareoption#1\ucc@enddeclareoption{%
    \DeclareOption{#1}{%
        \ClassError{uccthesis}%
                   {Option #1 disallowed.}%
                   {See package documentation for further information.}}%
}%
\@for\size:=1,2,3,4,5,6,7,8,9,13,14,15,16,17,18,19,20\do{
    \expandafter\ucc@declareoption\size pt\ucc@enddeclareoption%
}
\@for\option:=a5paper,b5paper,letterpaper,legalpaper,executivepaper,landscape\do{
    \expandafter\ucc@declareoption\option\ucc@enddeclareoption%
}
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

\DeclareOption{*}{\MoveOptionToClass{report}}

\ProcessOptions\relax

\LoadClass[\ucc@x@pt,
           \ucc@x@side,
           a4paper,
           openright,
           titlepage,
           fleqn]{book}
\usepackage{uccutil2}

\RequirePackage[\ucc@spacing]{setspace}
\RequirePackage{textcase} % for \MakeTextUppercase
\RequirePackage{url}
\RequirePackage{booktabs}
\RequirePackage{fancyhdr}
\RequirePackage{fourier}
\RequirePackage{amsthm}
\RequirePackage{amsmath}
\RequirePackage[latin1]{inputenc}
\RequirePackage[T1]{fontenc} 

\RequirePackage[style=authoryear,
                useprefix=true,
                block=space,
                backend=biber,
                language=british]{biblatex}
\renewcommand*{\bibopenparen}{[}
\renewcommand*{\bibcloseparen}{]}
\renewcommand*{\finalandcomma}{,}
\renewcommand*{\finalnamedelim}{, and~}
% 3em recommended by Bringhurst, p 80.
\renewcommand*\bibnamedash{\rule[0.48ex]{3em}{0.14ex}\space}
% Bug fix
% http://tex.stackexchange.com/questions/311426/bibliography-error-use-of-blxbblverbaddi-doesnt-match-its-definition-ve
\def\blx@maxline{77}

\RequirePackage[tight,english,dotted]{minitoc}
\RequirePackage{textcase} % MakeTextUppercase
\RequirePackage{microtype}
\RequirePackage[dvipsnames]{xcolor}
\RequirePackage[english]{babel}
\RequirePackage[scaled=0.87]{beramono}
\RequirePackage{chngpage,calc} % titlepage calculations
\RequirePackage{tikz}
\RequirePackage{pgfplots}
\RequirePackage{graphicx}
\RequirePackage{tocbibind}
\DeclareGraphicsExtensions{.pdf,.png,.gif,.jpg}

\usetikzlibrary{patterns}

\let\ucc@printbibliography=\printbibliography
\def\printbibliography{%
    \cleardoublepage
    \addcontentsline{toc}{chapter}{\bibname}%
    \ucc@printbibliography%
}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Hyphenation
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
\hyphenation{fortran}

\ucc@process@options

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Redefining \headheight to make fancyhdr happy.
\addtolength\textheight{-\headheight}
\setlength\headheight{14.49998pt}
\addtolength\textheight{\headheight}
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
\AtBeginDocument{\ucc@preamble}

\makeatother
