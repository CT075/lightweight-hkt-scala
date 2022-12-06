export TEXINPUTS:=$(TEXINPUTS)

FILE := writeup.tex

pdf: $(FILE)
	pdflatex $(FILE) && pdflatex $(FILE)

clean:
	rm *.pdf *.log *.aux *.out

