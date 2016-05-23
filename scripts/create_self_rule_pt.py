import sys, re

def create(jointRep):
	jointRepFiles = [open(f, 'r') for f in jointRep]
	
	uniq = set()

	for f in jointRepFiles:
		[uniq.add(word.strip()) for line in f for word in line.strip().split()]

	for word in uniq:
		if word == '#####':
			print '##### ||| # ||| 1 1 1 1 ||| 0-0 ||| 1 1 1 ||| |||'
		else:
			print word + ' ||| ' + word.rsplit('###')[0] + ' ||| 1 1 1 1 ||| 0-0 ||| 1 1 1 ||| |||'


if __name__ == '__main__':
	jointRep = sys.argv[1:]
	create(jointRep)
