from __future__ import division
from __future__ import print_function

import sys
from itertools import izip

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

def get_average(moses_ini):
	fr = []

	for ini in moses_ini:
		try:
			temp_fr = open(ini, 'r')
			if is_valid(temp_fr):
				temp_fr.seek(0)
				fr.append(temp_fr)
		except Exception as e:
			eprint(e)

	if(len(fr) == 0):
		eprint('There are no valid moses.ini files')
	else:	
		for seg in izip(*fr):
			toks = seg[0].strip().split(' ', 1)
			if toks[0] in features:
				weights = [s.strip().split(' ', 1)[1] for s in seg]
				print(toks[0] + ' ' + avg(weights))

		for f in fr:
			f.close()

def is_valid(f):
	for line in f:
		toks = line.strip().split(' ',1)
		if toks[0] in features:
			for w in toks[1].strip().split(' '):
				if float(w) == 0.0:
					return False
	return True

def avg(weights):
	data = [map(float, w.strip().split(' ')) for w in weights]
	average = [str(aith_avg(e)) for e in izip(*data)]
	return ' '.join(average)

def aith_avg(e):
	try:
		return sum(e)/len(e)
	except Exception as e:
			eprint(e)
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

if __name__ == '__main__':
	moses_ini = sys.argv[1:]
	get_average(moses_ini)
