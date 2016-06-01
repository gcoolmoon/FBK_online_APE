from __future__ import division
import sys
from itertools import izip

def airthmetic_average(moses_ini):
	fr = [open(ini, 'r') for ini in moses_ini]

	features = []
	features.append('LexicalReordering0=')
	features.append('Distortion0=')
	features.append('LM0=')
	features.append('LM1=')
	features.append('WordPenalty0=')
	features.append('PhrasePenalty0=')
	features.append('TranslationModel0=')
	features.append('TranslationModel1=')
	features.append('UnknownWordPenalty0=')

	for seg in izip(*fr):
		toks = seg[0].strip().split(' ', 1)
		if toks[0] in features:
			weights = [s.strip().split(' ', 1)[1] for s in seg]
			print toks[0] + ' ' + get_avg(weights)
		else:
			print seg[0].strip()

	for f in fr:
		f.close()

def get_avg(weights):
	data = [map(float, w.strip().split(' ')) for w in weights]
	average = [str(sum(e)/len(e)) for e in zip(*data)]
	return ' '.join(average)

if __name__ == '__main__':
	moses_ini = sys.argv[1:]
	airthmetic_average(moses_ini)
